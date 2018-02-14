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
import com.intellij.codeInsight.daemon.impl.analysis.HighlightMessageUtil;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightUtil;
import com.intellij.codeInsight.daemon.impl.analysis.JavaHighlightUtil;
import com.intellij.codeInsight.daemon.impl.quickfix.AccessStaticViaInstanceFix;
import com.intellij.codeInsight.daemon.impl.quickfix.RemoveUnusedVariableUtil;
        import com.intellij.codeInspection.InspectionManager;
        import com.intellij.codeInspection.LocalQuickFix;
        import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.accessStaticViaInstance.AccessStaticViaInstance;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.*;
        import com.intellij.util.containers.PrimitiveConvertor;
        import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * @author caikang
 * @date 2016/12/08
 */
public class AliAccessStaticViaInstanceInspection extends AccessStaticViaInstance implements AliBaseInspection {
    public String messageKey;

    public AliAccessStaticViaInstanceInspection() {
        this.messageKey = "com.alibaba.p3c.idea.inspection.standalone.AliAccessStaticViaInstanceInspection";
    }

    public AliAccessStaticViaInstanceInspection(@Nullable Object any) {
        this();
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return P3cBundle.getMessage(this.messageKey+".message");
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return P3cBundle.getMessage(this.messageKey+".desc");
    }

    @Override
    public String ruleName() {
        return "AvoidAccessStaticViaInstanceRule";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "AliAccessStaticViaInstance";
    }


    @Override
    protected AccessStaticViaInstanceFix createAccessStaticViaInstanceFix(PsiReferenceExpression expr, boolean onTheFly, final JavaResolveResult result) {
        return new AccessStaticViaInstanceFix(expr, result, onTheFly) {
            String fixKey = "com.alibaba.p3c.idea.quickfix.standalone.AliAccessStaticViaInstanceInspection";
            String text = calcText((PsiMember) result.getElement(), result.getSubstitutor());

            @NotNull
            @Override
            public String getText() {
                return text;
            }

            private String calcText(PsiMember member, PsiSubstitutor substitutor) {
                PsiClass aClass = member.getContainingClass();
                if (aClass == null) return "";
                P3cConfig p3cConfig = ServiceManager.getService(P3cConfig.class);
                if (p3cConfig.locale.equals(P3cConfig.localeZh)) {
                    return String.format(P3cBundle.getMessage(fixKey),
                            HighlightUtil.formatClass(aClass, false),
                            HighlightUtil.formatClass(aClass), HighlightMessageUtil.getSymbolName(member, substitutor));
                } else {
                    return String.format(P3cBundle.getMessage(fixKey), HighlightUtil.formatClass(aClass),
                            HighlightMessageUtil.getSymbolName(member, substitutor),
                            HighlightUtil.formatClass(aClass, false));
                }
            }
        };
    }


    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitReferenceExpression(PsiReferenceExpression expression) {
                checkAccessStaticMemberViaInstanceReference(expression, holder, isOnTheFly);
            }
        };
    }


    @NotNull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevels.BLOCKER;
    }


    private void checkAccessStaticMemberViaInstanceReference(PsiReferenceExpression expr, ProblemsHolder holder,Boolean onTheFly) {
        JavaResolveResult result = expr.advancedResolve(false);
        PsiElement psiElement=result.getElement();
        if (!(psiElement instanceof PsiMember)) {
            psiElement = null;
        }
        PsiMember resolved = (PsiMember)psiElement;
        if (resolved == null) return;
        PsiExpression qualifierExpression = expr.getQualifierExpression();
        if (qualifierExpression == null) return;

        if (qualifierExpression instanceof PsiReferenceExpression) {
            PsiElement qualifierResolved = ((PsiReferenceExpression) qualifierExpression).resolve();
            if (qualifierResolved instanceof PsiClass || qualifierResolved instanceof PsiPackage) {
                return;
            }
        }
        if (!resolved.hasModifierProperty(PsiModifier.STATIC)) {
            return;
        }

        String description = String.format(P3cBundle.getMessage(
                this.messageKey+".errMsg"),
                JavaHighlightUtil.formatType(qualifierExpression.getType())+"."+HighlightMessageUtil.getSymbolName(resolved, result.getSubstitutor()));
        if (!onTheFly) {
            if (RemoveUnusedVariableUtil.checkSideEffects(qualifierExpression, null, new ArrayList<PsiElement>())) {
                holder.registerProblem(expr, description);
                return;
            }
        }
        holder.registerProblem(expr, description,createAccessStaticViaInstanceFix(expr, onTheFly, result));
    }

    @Override
    public LocalQuickFix manualBuildFix(PsiElement psiElement, Boolean isOnTheFly) {
        return null;
    }

    @Override
    public PsiElement manualParsePsiElement(PsiFile psiFile, InspectionManager manager, int start, int end) {
        return  psiFile.findElementAt(start);
    }
}
