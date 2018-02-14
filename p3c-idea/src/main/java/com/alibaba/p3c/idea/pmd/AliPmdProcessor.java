/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.p3c.idea.pmd;

;
import com.google.common.base.Throwables;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.psi.PsiFile;
import com.sun.org.apache.regexp.internal.RE;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSets;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.RulesetsFactoryUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author caikang
 * @date 2016/12/11
 */
public class AliPmdProcessor {
    private static Logger LOG = Logger.getInstance(AliPmdProcessor.class);

    private RuleSetFactory ruleSetFactory;
    private PMDConfiguration configuration = new PMDConfiguration();
    private Rule rule;

    public AliPmdProcessor(Rule rule) {
        this.rule = rule;
        ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(configuration);
    }


    public List<RuleViolation> processFile(PsiFile psiFile) {
        configuration.setSourceEncoding(psiFile.getVirtualFile().getCharset().name());
        configuration.setInputPaths(psiFile.getVirtualFile().getCanonicalPath());
        Document document = FileDocumentManager.getInstance().getDocument(psiFile.getVirtualFile());
        if (document == null) return Collections.emptyList();
        final RuleContext ctx = new RuleContext();
        SourceCodeProcessor processor = new SourceCodeProcessor(configuration);
        String niceFileName = psiFile.getVirtualFile().getCanonicalPath();
//        if (niceFileName==null) throw new Exception("niceFileName is null ");

        Report report = Report.createReport(ctx, niceFileName);
        RuleSets ruleSets = new RuleSets();
        RuleSet ruleSet = new RuleSet();
        ruleSet.addRule(rule);
        ruleSets.addRuleSet(ruleSet);
        LOG.debug("Processing " + ctx.getSourceCodeFilename());
        ruleSets.start(ctx);
        try {
            ctx.setLanguageVersion(null);
            processor.processSourceCode(new StringReader(document.getText()), ruleSets, ctx);
        } catch (PMDException pmde) {
            LOG.debug("Error while processing file: " + niceFileName, pmde.getCause());
            report.addError(new Report.ProcessingError(pmde.getMessage(), niceFileName));
        } /*catch(IOException ioe){
            LOG.error("Unable to read source file: " + niceFileName, ioe);
        }*/ catch (RuntimeException re) {
            Throwable root = Throwables.getRootCause(re);
            if (!(root instanceof ApplicationUtil.CannotRunReadActionException)) {
                LOG.error("RuntimeException while processing file: " + niceFileName, re);
            }
        }
        ruleSets.end(ctx);
        Report ctxReport=ctx.getReport();
        Iterable<RuleViolation> iterable=(Iterable<RuleViolation>)ctxReport;
        Iterator<RuleViolation> iterator= iterable.iterator();
        List<RuleViolation> list=new ArrayList<>();
        while (iterator.hasNext()){
            list.add(iterator.next());
        }
        return list;
    }


}
