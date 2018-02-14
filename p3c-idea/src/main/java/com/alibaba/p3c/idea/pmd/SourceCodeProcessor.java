/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package com.alibaba.p3c.idea.pmd;

;
import com.alibaba.p3c.idea.config.P3cConfig;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSets;
import net.sourceforge.pmd.benchmark.Benchmark;
import net.sourceforge.pmd.benchmark.Benchmarker;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.LanguageVersionHandler;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.VisitorStarter;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.ParseException;
import net.sourceforge.pmd.lang.xpath.Initializer;
import org.apache.commons.io.IOUtils;

import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SourceCodeProcessor {

    Logger logger = Logger.getInstance(SourceCodeProcessor.class);

    private PMDConfiguration configuration;

    public SourceCodeProcessor(PMDConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Processes the input stream against a rule set using the given input encoding.
     * If the LanguageVersion is `null`  on the RuleContext, it will
     * be automatically determined.  Any code which wishes to process files for
     * different Languages, will need to be sure to either properly set the
     * Language on the RuleContext, or set it to `null` first.
     *
     * @param sourceCode The Reader to analyze.
     * @param ruleSets   The collection of rules to process against the file.
     * @param ctx        The context in which PMD is operating.
     * @throws PMDException if the input encoding is unsupported, the input stream could
     *                      not be parsed, or other error is encountered.
     * @see RuleContext.setLanguageVersion
     * @see PMDConfiguration.getLanguageVersionOfFile
     */
    public void processSourceCode(Reader sourceCode, RuleSets ruleSets, RuleContext ctx) throws PMDException {
        determineLanguage(ctx);

        // make sure custom XPath functions are initialized
        Initializer.initialize();
        // Coarse check to see if any RuleSet applies to file, will need to do a finer RuleSet specific check later
        try {
            processSource(sourceCode, ruleSets, ctx);

        } catch (ParseException pe){
            throw new PMDException("Error while parsing " + ctx.getSourceCodeFilename(), pe);
        } catch(Exception e){
            throw new PMDException("Error while processing " + ctx.getSourceCodeFilename(), e);
        } finally{
            IOUtils.closeQuietly(sourceCode);
        }
    }


    private Node parse(RuleContext ctx, Reader sourceCode, Parser parser){
        long start = System.nanoTime();
        Node rootNode = parser.parse(ctx.getSourceCodeFilename(), sourceCode);
        ctx.getReport().suppress(parser.getSuppressMap());
        long end = System.nanoTime();
        Benchmarker.mark(Benchmark.Parser, end - start, 0);
        return rootNode;
    }

    private void symbolFacade(Node rootNode, LanguageVersionHandler languageVersionHandler) {
        long start = System.nanoTime();
        languageVersionHandler.getSymbolFacade(configuration.getClassLoader()).start(rootNode);
        long end = System.nanoTime();
        Benchmarker.mark(Benchmark.SymbolTable, end - start, 0);
    }

    private void usesDFA(LanguageVersion languageVersion, Node rootNode, RuleSets ruleSets) {
        if (ruleSets.usesDFA(languageVersion.getLanguage())) {
            long start = System.nanoTime();
            VisitorStarter dataFlowFacade = languageVersion.getLanguageVersionHandler().getDataFlowFacade();
            dataFlowFacade.start(rootNode);
            long end = System.nanoTime();
            Benchmarker.mark(Benchmark.DFA, end - start, 0);
        }
    }

    private void usesTypeResolution(LanguageVersion languageVersion, Node rootNode, RuleSets  ruleSets) {
        if (ruleSets.usesTypeResolution(languageVersion.getLanguage())) {
            long start = System.nanoTime();
            languageVersion.getLanguageVersionHandler().getTypeResolutionFacade(configuration.getClassLoader()).start(rootNode);
            long end = System.nanoTime();
            Benchmarker.mark(Benchmark.TypeResolution, end - start, 0);
        }
    }

    private void processSource(Reader sourceCode, RuleSets ruleSets, RuleContext ctx) {
        long start = System.currentTimeMillis();
        List<Node> acus = Collections.singletonList(getRootNode(sourceCode, ruleSets, ctx));
        logger.debug("elapsed ${System.currentTimeMillis() - start}ms to" +
                " parse ast tree for file ${ctx.sourceCodeFilename}");
        ruleSets.apply(acus, ctx, ctx.getLanguageVersion().getLanguage());
    }

    private Node getRootNode(Reader sourceCode, RuleSets ruleSets, RuleContext ctx){
        if (!smartFoxConfig.isAstCacheEnable()) {
            return parseNode(ctx, ruleSets, sourceCode);
        }
        Node node = nodeCache.getIfPresent(ctx.getSourceCodeFilename());
        if (node != null) {
            return node;
        }
        return parseNode(ctx, ruleSets, sourceCode);
    }

    private Node parseNode(RuleContext ctx, RuleSets ruleSets,Reader sourceCode){
        LanguageVersion languageVersion = ctx.getLanguageVersion();
        LanguageVersionHandler languageVersionHandler = languageVersion.getLanguageVersionHandler();
        Parser parser = PMD.parserFor(languageVersion, configuration);
        Node rootNode = parse(ctx, sourceCode, parser);
        symbolFacade(rootNode, languageVersionHandler);
        usesDFA(languageVersion, rootNode, ruleSets);
        usesTypeResolution(languageVersion, rootNode, ruleSets);
        nodeCache.put(ctx.getSourceCodeFilename(), rootNode);
        return rootNode;
    }

    private void determineLanguage(RuleContext ctx) {
        // If LanguageVersion of the source file is not known, make a determination
        if (ctx.getLanguageVersion() == null) {
            LanguageVersion languageVersion = configuration.getLanguageVersionOfFile(ctx.getSourceCodeFilename());
            ctx.setLanguageVersion(languageVersion);
        }
    }


    public static P3cConfig smartFoxConfig = ServiceManager.getService(P3cConfig.class);
    private static Cache<String, Node> nodeCache;


    static {
        try {
            reInitNodeCache(smartFoxConfig.getAstCacheTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reInitNodeCache(Long expireTime) throws Exception {
        nodeCache = CacheBuilder.newBuilder().concurrencyLevel(16)
                .expireAfterWrite(expireTime, TimeUnit.MILLISECONDS)
                .maximumSize(100)
                .build();
        if (nodeCache == null) {
            throw new Exception("nodeCache is null");
        }
    }

    public static void invalidateCache(String file) {
        nodeCache.invalidate(file);
    }

}
