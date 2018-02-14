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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.numeric.LongLiteralsEndingWithLowercaseLInspection;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 * Batch QuickFix Supported
 * @author caikang
 * @date 2017/01/20
 */
public class AliLongLiteralsEndingWithLowercaseLInspection extends LongLiteralsEndingWithLowercaseLInspection implements AliBaseInspection {
    public AliLongLiteralsEndingWithLowercaseLInspection() {
    }

    public AliLongLiteralsEndingWithLowercaseLInspection(@Nullable Object any) {
        this();
    }

    @NotNull
    @Override
    public String ruleName() {
        return "UpperEllRule";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return RuleInspectionUtils.getRuleMessage(this.ruleName());
    }

    @NotNull
    @Override
    protected String buildErrorString(@NotNull Object... infos) {
        Intrinsics.checkParameterIsNotNull(infos, "infos");
        return P3cBundle.getMessage("com.alibaba.p3c.idea.inspection.rule.AliLongLiteralsEndingWithLowercaseL.errMsg");
    }

    @NotNull
    @Override
    public String getShortName() {
        return "AliLongLiteralsEndingWithLowercaseL";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return RuleInspectionUtils.getRuleStaticDescription(this.ruleName());
    }

    @NotNull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return RuleInspectionUtils.getHighlightDisplayLevel(this.ruleName());
    }

    @Override public InspectionGadgetsFix buildFix(Object... infos){
        InspectionGadgetsFix fix = super.buildFix(infos);
        if (fix==null) return null;
        return new DecorateInspectionGadgetsFix(fix,
                P3cBundle.getMessage("com.alibaba.p3c.idea.quickfix.AliLongLiteralsEndingWithLowercaseL"));
    }

    @Override public LocalQuickFix manualBuildFix(PsiElement psiElement,Boolean isOnTheFly) {
        return buildFix(psiElement);
    }

    @Override
    public PsiElement manualParsePsiElement(PsiFile psiFile ,InspectionManager manager , int start, int end)  {
        return psiFile.findElementAt(start).getParent();
    }
}
