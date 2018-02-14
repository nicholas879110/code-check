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
import com.alibaba.p3c.idea.i18n.P3cBundle;
import com.alibaba.p3c.idea.quickfix.DecorateInspectionGadgetsFix;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.*;
import com.siyeh.HardcodedMethodConstants;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.TypeUtils;
import com.siyeh.ig.style.LiteralAsArgToStringEqualsInspection;
import kotlin.TypeCastException;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Batch QuickFix Supported
 *
 * @author caikang
 * @date 2017/02/27
 */
public class AliEqualsAvoidNullInspection extends LiteralAsArgToStringEqualsInspection implements AliBaseInspection {
    public AliEqualsAvoidNullInspection() {
    }

    public AliEqualsAvoidNullInspection(@Nullable Object any) {
        this();
    }

    @NotNull
    public String ruleName() {
        return "EqualsAvoidNullRule";
    }

    @NotNull
    public String getDisplayName() {
        return RuleInspectionUtils.getRuleMessage(this.ruleName());
    }

    @NotNull
    public String buildErrorString(@NotNull Object... infos) {
        Intrinsics.checkParameterIsNotNull(infos, "infos");
        Object methodName = infos[0];
        if (methodName == null) {
            throw new TypeCastException("null cannot be cast to non-null type kotlin.String");
        } else {
            return String.format(P3cBundle.getMessage("com.alibaba.p3c.idea.inspection.rule.AliEqualsAvoidNull.errMsg"),
                    (String) methodName);

        }
    }

    @NotNull
    public String getShortName() {
        return "AliEqualsAvoidNull";
    }

    @Nullable
    public String getStaticDescription() {
        return RuleInspectionUtils.getRuleStaticDescription(this.ruleName());
    }

    @NotNull
    public HighlightDisplayLevel getDefaultLevel() {
        return RuleInspectionUtils.getHighlightDisplayLevel(this.ruleName());
    }

    @NotNull
    public BaseInspectionVisitor buildVisitor() {
        return (BaseInspectionVisitor) (new AliEqualsAvoidNullInspection.LiteralAsArgToEqualsVisitor());
    }

    private class LiteralAsArgToEqualsVisitor extends BaseInspectionVisitor {

        @Override
        public void visitMethodCallExpression(
                PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            @NonNls String methodName = methodExpression.getReferenceName();
            if (HardcodedMethodConstants.EQUALS != methodName && HardcodedMethodConstants.EQUALS_IGNORE_CASE != methodName) {
                return;
            }
            PsiExpressionList argList = expression.getArgumentList();
            PsiExpression[] args = argList.getExpressions();
            if (args.length != 1) {
                return;
            }
            PsiExpression argument = args[0];
            PsiType argumentType = argument.getType();
            if (argumentType == null) return;
            if (!(argument instanceof PsiLiteralExpression) && !isConstantField(argument)) {
                return;
            }
            if (!TypeUtils.isJavaLangString(argumentType)) {
                return;
            }
            PsiExpression target = methodExpression.getQualifierExpression();
            if (target instanceof PsiLiteralExpression || isConstantField(argument)) {
                return;
            }
            registerError(argument, methodName);
        }

        private Boolean isConstantField(PsiExpression argument) {
            if (!(argument instanceof PsiReferenceExpression)) {
                return false;
            }
            PsiField psiField = (PsiField) ((PsiReferenceExpression) argument).resolve();
            if (psiField == null) {
                return false;
            }
            PsiModifierList modifierList = psiField.getModifierList();
            if (modifierList == null) return false;
            return modifierList.hasModifierProperty("final") && modifierList.hasModifierProperty("static");
        }
    }

    @Override
    public InspectionGadgetsFix buildFix(Object... infos) {
        InspectionGadgetsFix fix = super.buildFix(infos);
        if (fix == null) return null;
        return new DecorateInspectionGadgetsFix(fix,
                P3cBundle.getMessage("com.alibaba.p3c.idea.quickfix.AliEqualsAvoidNull"));
    }

    @Override
    public LocalQuickFix manualBuildFix(PsiElement psiElement, Boolean isOnTheFly) {
        return buildFix(psiElement);
    }

    @Override
    public PsiElement manualParsePsiElement(PsiFile psiFile, InspectionManager manager, int start, int end) {
        return psiFile.findElementAt(start).getParent().getParent();
    }
}
