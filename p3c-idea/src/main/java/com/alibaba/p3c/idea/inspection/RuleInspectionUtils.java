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
package com.alibaba.p3c.idea.inspection;

;
import com.alibaba.p3c.idea.config.P3cConfig;
import com.alibaba.p3c.idea.util.HighlightDisplayLevels;
import com.alibaba.p3c.idea.util.NumberConstants;
import com.alibaba.p3c.pmd.I18nResources;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.URLUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import kotlin.jvm.internal.Intrinsics;
import net.sourceforge.pmd.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * @author caikang
 * @date 2016/12/16
 */
public class RuleInspectionUtils {

    private static final Logger logger = Logger.getInstance(RuleInspectionUtils.class);
    private static final Pattern ruleSetFilePattern = Pattern.compile("(java|vm)/ali-.*?\\.xml");

    private static Template staticDescriptionTemplate;

    private static final String ruleSetsPrefix = "rulesets/";

    private static Map<String, String> ruleStaticDescriptions;
    private static Map<String, String> ruleMessages;
    private static Map<String, HighlightDisplayLevel> displayLevelMap;

    static {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setClassForTemplateLoading(RuleInspectionUtils.class, "/tpl");
        cfg.setDefaultEncoding("UTF-8");
        try {
            staticDescriptionTemplate = cfg.getTemplate("StaticDescriptionTemplate.ftl");
        } catch (IOException e) {
            e.printStackTrace();
        }

        I18nResources.changeLanguage(ServiceManager.getService(P3cConfig.class).locale);
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        ImmutableMap.Builder<String, String> messageBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<String, HighlightDisplayLevel> displayLevelBuilder = ImmutableMap.builder();
        List<Rule> rules = loadAllAlibabaRule();
        for (Rule rule : rules) {
            builder.put(rule.getName(), parseStaticDescription(rule));
            messageBuilder.put(rule.getName(), rule.getMessage());
            displayLevelBuilder.put(rule.getName(), getHighlightDisplayLevel(rule.getPriority()));
        }
        ruleStaticDescriptions = builder.build();
        ruleMessages = messageBuilder.build();
        displayLevelMap = displayLevelBuilder.build();
    }

    public static String getRuleStaticDescription(String ruleName){
        if (ruleStaticDescriptions.get(ruleName)==null){
            throw new NullPointerException();
        }
        return ruleStaticDescriptions.get(ruleName);
    }

    public static HighlightDisplayLevel getHighlightDisplayLevel(String ruleName){
        if (ruleName==null){
            throw new NullPointerException();
        }
        HighlightDisplayLevel level = displayLevelMap.get(ruleName);
        if (level==null){
            level=HighlightDisplayLevel.WEAK_WARNING;
        }
        return level;
    }

    public static HighlightDisplayLevel getHighlightDisplayLevel(RulePriority rulePriority){
        if (rulePriority.equals( RulePriority.HIGH)){
            return HighlightDisplayLevels.BLOCKER;
        }else if (rulePriority.equals(RulePriority.MEDIUM_HIGH)){
            return HighlightDisplayLevels.CRITICAL;
        }else {
            return HighlightDisplayLevels.MAJOR;
        }
    }

    public static String getRuleMessage(String ruleName){
        if (ruleName==null){
            throw new NullPointerException();
        }
        String msg=ruleMessages.get(ruleName);
        if (msg==null){
            throw new NullPointerException();
        }
        return msg;
    }

    private static String parseStaticDescription(Rule rule) {
        StringWriter writer = new StringWriter();
        try {
            Map<String, Object> map = Maps.newHashMap();
            map.put("message", StringUtils.trimToEmpty(rule.getMessage()));
            map.put("description", StringUtils.trimToEmpty(rule.getDescription()));
            List examples =new ArrayList();
            for (String it:rule.getExamples()){
                examples.add(it.trim());//???????
            }
            map.put("examples", examples);
            staticDescriptionTemplate.process(map, writer);
        } catch (TemplateException e){
            logger.error(e);
        } catch(IOException e){
            logger.error(e);
        }
        return writer.toString();
    }

    private static List<Rule> loadAllAlibabaRule() {
        try {
            Thread.currentThread().setContextClassLoader(RuleInspectionUtils.class.getClassLoader());
            List<String> ruleSetConfigs = findRuleSetConfigs();
            RuleSetFactory ruleSetFactory = new RuleSetFactory();
            RuleSets ruleSets = ruleSetFactory.createRuleSets(
                    Joiner.on(",").join(ruleSetConfigs).replace("/", "-"));
            Map<String, Rule> map = Maps.newHashMap();

            //?????????
            RuleSet[] ruleSets1 = ruleSets.getAllRuleSets();
            for (RuleSet it : ruleSets1) {
                Collection<Rule> rules = it.getRules();
                for (Rule rule : rules) {
                    map.put(rule.getName(), rule);
                }
            }
            //?????????
            return Lists.newArrayList(map.values());
        } catch (IOException e) {
            logger.warn("no available alibaba rules");
            return Collections.emptyList();
        } catch (RuleSetNotFoundException e) {
            logger.error("rule sets not found", e);
            return Collections.emptyList();
        }

    }


    private static List<String> findRuleSetConfigs() throws IOException {
        List<String> ruleSets = Lists.newArrayList();
        Enumeration<URL> enumeration = RuleInspectionUtils.class.getClassLoader().getResources(ruleSetsPrefix);
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            if (Intrinsics.areEqual(URLUtil.JAR_PROTOCOL, url.getProtocol())) {
                findRuleSetsFromJar(ruleSets, url);
            } else if (Intrinsics.areEqual(URLUtil.FILE_PROTOCOL, url.getProtocol())) {
                findRuleSetsFromDirectory(ruleSets, url);
            }
        }
        return ruleSets;
    }


    private static void findRuleSetsFromDirectory(List<String> ruleSets, URL url) throws IOException {
        File file = new File(url.getPath());
        if (file.exists() && file.isDirectory()) {
            List<File> files = Lists.newArrayList();
            FileUtil.collectMatchedFiles(file, ruleSetFilePattern, files);
            for (File it : files) {
                String resultPath = it.getCanonicalPath().replace(url.getPath(), "").replace(".xml", "");
                ruleSets.add(resultPath);
            }
        }
    }


    private static void findRuleSetsFromJar(List<String> ruleSets, URL url) throws IOException {
        logger.info("start to find rule sets from jar " + url);
        String path = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name());
        int index = path.lastIndexOf(URLUtil.JAR_SEPARATOR);
        if (index > NumberConstants.INDEX_0) {
            path = path.substring("file:".length(), index);
        }
        JarFile jarFile = new JarFile(path);
        logger.info("create jarFile for path " + path);
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String subPath = jarEntry.getName().replace(ruleSetsPrefix, "");
            if (ruleSetFilePattern.matcher(subPath).find()) {
                String resultPath = subPath.replace(".xml", "");
                logger.info("get result rule set " + resultPath);
                ruleSets.add(resultPath);
            }
        }
        logger.info("find rule sets from jar $url finished");
    }
}
