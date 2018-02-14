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
package com.alibaba.p3c.idea.inspection.standalone;

;
import com.alibaba.p3c.idea.config.P3cConfig;
import com.alibaba.p3c.idea.i18n.P3cBundle;
import com.alibaba.p3c.idea.inspection.AliBaseInspection;
import com.alibaba.p3c.idea.util.HighlightDisplayLevels;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.deprecation.DeprecationInspection;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * @author caikang
 * @date 2016/12/08
 */
public class AliDeprecationInspection extends DeprecationInspection implements AliBaseInspection {
    public String messageKey = "com.alibaba.p3c.idea.inspection.standalone.AliDeprecationInspection";

    public AliDeprecationInspection() {
    }

    public AliDeprecationInspection(Object any) {
        this();
    }

    public static boolean IGNORE_INSIDE_DEPRECATED = false;
    public static boolean IGNORE_ABSTRACT_DEPRECATED_OVERRIDES = false;
    public static boolean IGNORE_IMPORT_STATEMENTS = false;
    public static boolean IGNORE_METHODS_OF_DEPRECATED = false;

    @NotNull
    @Override
    public String getDisplayName() {
        return P3cBundle.getMessage(this.messageKey+".message");
    }

    @Override
    public String ruleName() {
        return "AvoidUseDeprecationApiRule";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "AliDeprecation";
    }



    @Nullable
    @Override
    public String getStaticDescription() {
        return P3cBundle.getMessage(this.messageKey+".desc");
    }

    @Override
    public JComponent createOptionsPanel() {
        return null;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        P3cConfig p3cConfig = ServiceManager.getService(P3cConfig.class);
        if (p3cConfig.locale.equals(P3cConfig.localeEn)) {
            return super.buildVisitor(holder, isOnTheFly);
        } else {
            return super.buildVisitor(new DeprecationInspectionProblemsHolder(holder, isOnTheFly), isOnTheFly);
        }

    }

    @NotNull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevels.CRITICAL;
    }


    class DeprecationInspectionProblemsHolder extends ProblemsHolder {
        private ProblemsHolder holder;

        public DeprecationInspectionProblemsHolder(@NotNull ProblemsHolder holder, boolean onTheFly) {
            super(holder.getManager(), holder.getFile(), onTheFly);
            this.holder = holder;
        }


        public void registerProblem(@NotNull PsiElement psiElement, @NotNull /*@Nls(capitalization = Nls.Capitalization.Sentence) */String descriptionTemplate, LocalQuickFix... fixes) {
            holder.registerProblem(psiElement, getMessage(descriptionTemplate), fixes);
        }


        public void registerProblem(PsiElement psiElement, /*@Nls(capitalization = Nls.Capitalization.Sentence) */String descriptionTemplate,
                                    ProblemHighlightType highlightType, LocalQuickFix... fixes) {
            holder.registerProblem(psiElement, getMessage(descriptionTemplate), highlightType, fixes);
        }

        @Override
        public void registerProblem(PsiReference reference, String descriptionTemplate, ProblemHighlightType highlightType) {
            holder.registerProblem(reference, getMessage(descriptionTemplate), highlightType);
        }

        @Override
        public void registerProblemForReference(PsiReference reference,
                                                ProblemHighlightType highlightType, String descriptionTemplate,
                                                LocalQuickFix... fixes) {
            holder.registerProblemForReference(reference, highlightType, getMessage(descriptionTemplate), fixes);
        }

        @Override
        public void registerProblem(PsiElement psiElement, TextRange rangeInElement, String message, LocalQuickFix... fixes) {
            holder.registerProblem(psiElement, rangeInElement, getMessage(message), fixes);
        }

        @Override
        public void registerProblem(PsiElement psiElement, String message, ProblemHighlightType highlightType, TextRange rangeInElement, LocalQuickFix... fixes) {
            holder.registerProblem(psiElement, getMessage(message), highlightType, rangeInElement, fixes);
        }

        private String getMessage(String msg) {
            return msg.replace("is deprecated", "已经过时了").replace("Default constructor in", "默认构造函数")
                    .replace("Overrides deprecated method in", "重写了过时的方法") + " #loc";
        }
    }

    @Nullable
    @Override
    public LocalQuickFix manualBuildFix(@NotNull PsiElement psiElement, Boolean isOnTheFly) {
        return null;
    }

    @NotNull
    public PsiElement manualParsePsiElement(@NotNull PsiFile psiFile, @NotNull InspectionManager manager, int start, int end) {
        return psiFile.findElementAt(start);
    }

}
