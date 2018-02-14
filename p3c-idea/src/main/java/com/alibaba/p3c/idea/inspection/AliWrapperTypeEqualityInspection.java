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
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.PsiReplacementUtil;
import com.siyeh.ig.fixes.EqualityToEqualsFix;
import com.siyeh.ig.psiutils.ComparisonUtils;
import com.siyeh.ig.psiutils.TypeUtils;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * Batch QuickFix Supported
 *
 * @author caikang
 * @date 2017/02/27
 */
public class AliWrapperTypeEqualityInspection extends BaseInspection implements AliBaseInspection {
    public AliWrapperTypeEqualityInspection() {
    }

    public AliWrapperTypeEqualityInspection(@Nullable Object any) {
        this();
    }

    public String familyName = replaceWith+" equals";

    @Override
    public String buildErrorString(Object... infos) {
        return P3cBundle.getMessage("com.alibaba.p3c.idea.inspection.rule.WrapperTypeEqualityRule.errMsg");
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new ObjectComparisonVisitor();
    }

    @Override
    public String ruleName() {
        return "WrapperTypeEqualityRule";
    }

    @Override
    public String getDisplayName() {
        return RuleInspectionUtils.getRuleMessage(ruleName());
    }

    @Override
    public String getShortName() {
        return "AliWrapperTypeEquality";
    }

    @Override
    public String getStaticDescription() {
        return RuleInspectionUtils.getRuleStaticDescription(ruleName());
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return RuleInspectionUtils.getHighlightDisplayLevel(ruleName());
    }

    @Override
    public InspectionGadgetsFix buildFix(Object... infos) {
        if (infos == null || infos.length == 0) {
            return new DecorateInspectionGadgetsFix(new EqualityToEqualsFix(), familyName);
        }
        PsiArrayType type = (PsiArrayType) infos[0];
        PsiType componentType = type.getComponentType();
        ArrayEqualityFix fix = new ArrayEqualityFix(componentType instanceof PsiArrayType);
        return new DecorateInspectionGadgetsFix(fix, fix.getName(), familyName);
    }

    private class ObjectComparisonVisitor extends BaseInspectionVisitor {
        @Override
        public void visitBinaryExpression(PsiBinaryExpression expression) {
            if (!ComparisonUtils.isEqualityComparison(expression)) {
                return;
            }
            checkForWrapper(expression);
        }

        private void checkForWrapper(PsiBinaryExpression expression) {
            PsiExpression rhs = expression.getROperand();
            if (rhs == null) return;
            PsiExpression lhs = expression.getLOperand();
            if (!isWrapperType(lhs) || !isWrapperType(rhs)) {
                return;
            }
            registerError(expression.getOperationSign());
        }

        private Boolean isWrapperType(PsiExpression expression) {
            if (hasNumberType(expression)) {
                return true;
            }
            return TypeUtils.expressionHasTypeOrSubtype(expression, CommonClassNames.JAVA_LANG_BOOLEAN)
                    || TypeUtils.expressionHasTypeOrSubtype(expression, CommonClassNames.JAVA_LANG_CHARACTER);
        }


        private Boolean hasNumberType(PsiExpression expression) {
            return TypeUtils.expressionHasTypeOrSubtype(expression, CommonClassNames.JAVA_LANG_NUMBER);
        }
        /**
         * checkForNumber end
         */

    }

    private class ArrayEqualityFix extends InspectionGadgetsFix {

        private Boolean deepEquals;

        public ArrayEqualityFix(Boolean deepEquals) {
            this.deepEquals = deepEquals;
        }


        @Override
        public String getName() {
            if (deepEquals) {
                return replaceWith+" 'Arrays.deepEquals()'";
            } else {
                return replaceWith+" 'Arrays.equals()'";
            }
        }

        @Override
        public String getFamilyName() {
            return familyName;
        }


        @Override
        public void doFix(Project project, ProblemDescriptor descriptor) throws IncorrectOperationException {
            PsiElement element = descriptor.getPsiElement();
            PsiBinaryExpression parent = (PsiBinaryExpression) (element.getParent());
            if (parent == null) return;
            IElementType tokenType = parent.getOperationTokenType();
            @NonNls StringBuilder newExpressionText = new StringBuilder();
            if (Intrinsics.areEqual(JavaTokenType.NE, tokenType)) {
                newExpressionText.append('!');
            } else if (!Intrinsics.areEqual(JavaTokenType.EQEQ, tokenType)) {
                return;
            }
            if (deepEquals) {
                newExpressionText.append("java.util.Arrays.deepEquals(");
            } else {
                newExpressionText.append("java.util.Arrays.equals(");
            }
            newExpressionText.append(parent.getLOperand().getText());
            newExpressionText.append(',');
            PsiExpression rhs = parent.getROperand();
            if (rhs == null) return;
            newExpressionText.append(rhs.getText());
            newExpressionText.append(')');
            PsiReplacementUtil.replaceExpressionAndShorten(parent,
                    newExpressionText.toString());
        }
    }

    @Override
    public LocalQuickFix manualBuildFix(PsiElement psiElement, Boolean isOnTheFly){
        PsiBinaryExpression expression = (PsiBinaryExpression) (psiElement.getParent());
        if (expression == null) return null;
        PsiExpression rhs = expression.getROperand();
        if (rhs == null) return null;
        PsiExpression lhs = expression.getLOperand();
        PsiType lhsType = lhs.getType();
        if (!(lhsType instanceof PsiArrayType) || !(rhs.getType() instanceof PsiArrayType)) {
            return buildFix();
        }
        return buildFix(lhsType);
    }

    public static final String replaceWith = P3cBundle.getMessage("com.alibaba.p3c.idea.quickfix.replace.with");

    @Override
    public PsiElement manualParsePsiElement(PsiFile psiFile, InspectionManager manager, int start, int end) {
        return psiFile.findElementAt(start).getParent();
    }
}
