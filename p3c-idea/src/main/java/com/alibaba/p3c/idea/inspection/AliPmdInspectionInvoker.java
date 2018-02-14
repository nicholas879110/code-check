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
import com.alibaba.p3c.idea.pmd.AliPmdProcessor;
import com.alibaba.p3c.idea.util.DocumentUtils;
import com.alibaba.p3c.idea.util.ProblemsUtils;
import com.beust.jcommander.internal.Lists;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleViolation;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author caikang
 * @date 2016/12/13
 */
public class AliPmdInspectionInvoker {


    Logger logger = Logger.getInstance(AliPmdInspectionInvoker.class);

    private PsiFile psiFile;
    private InspectionManager manager;
    private Rule rule;

    public AliPmdInspectionInvoker(PsiFile psiFile, InspectionManager manager, Rule rule) {
        this.psiFile = psiFile;
        this.manager = manager;
        this.rule = rule;
    }

    private List<RuleViolation> violations = Collections.emptyList();

    public void doInvoke() {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        AliPmdProcessor processor = new AliPmdProcessor(rule);
        long start = System.currentTimeMillis();
        violations = processor.processFile(psiFile);
        logger.info("elapsed ${System.currentTimeMillis() - start}ms to" +
                " to apply rule ${rule.name} for file ${psiFile.virtualFile.canonicalPath}");
    }

    public ProblemDescriptor[] getRuleProblems(Boolean isOnTheFly) {
        if (violations.isEmpty()) {
            return null;
        }
        List<ProblemDescriptor> problemDescriptors = Lists.newArrayList(violations.size());
        for (RuleViolation rv : violations) {
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(rv.getFilename());
            if (virtualFile == null) continue;
            PsiFile psiFile = PsiManager.getInstance(manager.getProject()).findFile(virtualFile);
            if (psiFile == null) continue;
            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null) continue;
            int offset = DocumentUtils.calculateRealOffset(document, rv.getBeginLine(), rv.getBeginColumn());
            int endOffset = DocumentUtils.calculateRealOffset(document, rv.getEndLine(), rv.getEndColumn());
            String errorMessage = "";
            if (isOnTheFly) {
                errorMessage = rv.getDescription();
            } else {
                errorMessage =rv.getDescription()+ " (line "+rv.getBeginLine()+")";
            }
            ProblemDescriptor problemDescriptor = ProblemsUtils.createProblemDescriptorForPmdRule(psiFile, manager,
                    isOnTheFly, rv.getRule().getName(), errorMessage, offset, endOffset, rv.getBeginLine());
            if (problemDescriptor == null) continue;
            problemDescriptors.add(problemDescriptor);
        }
        ProblemDescriptor[] problemDescriptorArray = new ProblemDescriptor[problemDescriptors.size()];
        problemDescriptorArray = problemDescriptors.toArray(problemDescriptorArray);
        return problemDescriptorArray;
    }


    public static Cache<FileRule, AliPmdInspectionInvoker> invokers;


    public static P3cConfig smartFoxConfig = ServiceManager.getService(P3cConfig.class);

    static {
        reInitInvokers(smartFoxConfig.getRuleCacheTime());
    }

    public static ProblemDescriptor[] invokeInspection(PsiFile psiFile, InspectionManager manager,Rule rule,Boolean  isOnTheFly){
        if (psiFile == null) {
            return null;
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile==null)return null;
        if (!smartFoxConfig.isRuleCacheEnable()) {
            AliPmdInspectionInvoker invoker = new AliPmdInspectionInvoker(psiFile, manager, rule);
            invoker.doInvoke();
            return invoker.getRuleProblems(isOnTheFly);
        }
        AliPmdInspectionInvoker invoker = invokers.getIfPresent(new FileRule(virtualFile.getCanonicalPath(), rule.getName()));
        if (invoker == null) {
            synchronized (virtualFile) {
                invoker = invokers.getIfPresent(virtualFile.getCanonicalPath());
                if (invoker == null) {
                    invoker = new AliPmdInspectionInvoker(psiFile, manager, rule);
                    invoker.doInvoke();
                    invokers.put(new FileRule(virtualFile.getCanonicalPath(), rule.getName()),invoker);
                }
            }
        }
        return invoker .getRuleProblems(isOnTheFly);
    }

    private static void doInvokeIfPresent(String filePath, String rule) {
        invokers.getIfPresent(new FileRule(filePath, rule)).doInvoke();
    }

    public static void refreshFileViolationsCache(VirtualFile file) {
        for (String it:AliLocalInspectionToolProvider.ruleNames){
            doInvokeIfPresent(file.getCanonicalPath(), it);
        }
    }

    public static void reInitInvokers(Long expireTime) {
        invokers = CacheBuilder.newBuilder().maximumSize(500).expireAfterWrite(expireTime,TimeUnit.MILLISECONDS).build();
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    public void setPsiFile(PsiFile psiFile) {
        this.psiFile = psiFile;
    }

    public InspectionManager getManager() {
        return manager;
    }

    public void setManager(InspectionManager manager) {
        this.manager = manager;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }
}


class FileRule {
    private String filePath;
    private String ruleName;

    public FileRule() {
    }

    public FileRule(String filePath, String ruleName) {
        this.filePath = filePath;
        this.ruleName = ruleName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
}
