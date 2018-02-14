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
import com.alibaba.p3c.idea.inspection.AliLocalInspectionToolProvider.ShouldInspectChecker;
import com.alibaba.p3c.idea.util.NumberConstants;
import com.alibaba.p3c.idea.util.QuickFixes;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.jvm.internal.Intrinsics;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RulePriority;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author caikang
 * @date 2016/12/16
 */
public class AliPmdInspection extends LocalInspectionTool implements AliBaseInspection, PmdRuleInspectionIdentify {


    @Override
    @Nullable
    public LocalQuickFix manualBuildFix(PsiElement psiElement, Boolean isOnTheFly) {
        Intrinsics.checkParameterIsNotNull(psiElement, "psiElement");
        return QuickFixes.getQuickFix(this.ruleName, isOnTheFly);
    }

    @Override
    public PsiElement manualParsePsiElement(PsiFile psiFile, InspectionManager manager, int start, int end) {
        return  psiFile.findElementAt(start);
    }

    private String ruleName;

    private String staticDescription;

    private String displayName;

    private ShouldInspectChecker shouldInspectChecker;

    private HighlightDisplayLevel defaultLevel;

    private Rule rule;

    public AliPmdInspection(@NotNull String ruleName) {
        this.ruleName = ruleName;
        staticDescription = RuleInspectionUtils.getRuleStaticDescription(ruleName);
        AliLocalInspectionToolProvider.RuleInfo ruleInfo = AliLocalInspectionToolProvider.getRuleInfoMap().get(ruleName);
        shouldInspectChecker = ruleInfo.getShouldInspectChecker();
        rule = ruleInfo.getRule();
        displayName = rule.getMessage();
        defaultLevel = RuleInspectionUtils.getHighlightDisplayLevel(rule.getPriority());
    }


    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @Override
    public ProblemDescriptor[] checkFile(PsiFile file, InspectionManager manager,
                                         boolean isOnTheFly) {
        if (!shouldInspectChecker.shouldInspect(file)) {
            return null;
        }
        return AliPmdInspectionInvoker.invokeInspection(file, manager, rule, isOnTheFly);
    }

    @Override
    public String getStaticDescription() {
        return staticDescription;
    }

    @Override
    public String ruleName() {
        return ruleName;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel()

    {
        return defaultLevel;
    }

    @Nls
    @Override
    public String getGroupDisplayName() {
        return AliBaseInspection.GROUP_NAME;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public boolean isSuppressedFor(PsiElement element) {
        return false;
    }

    @Override
    public String getShortName() {
        String shortName = "Alibaba" + ruleName;
        int index = shortName.lastIndexOf("Rule");
        if (index > NumberConstants.INDEX_0) {
            shortName = shortName.substring(NumberConstants.INDEX_0, index);
        }
        return shortName;
    }
}
