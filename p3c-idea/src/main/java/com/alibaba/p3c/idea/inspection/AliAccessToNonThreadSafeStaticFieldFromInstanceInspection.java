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

import com.alibaba.p3c.idea.i18n.P3cBundle;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.siyeh.ig.threading.AccessToNonThreadSafeStaticFieldFromInstanceInspectionBase;
import org.jetbrains.annotations.Nullable;

/**
 * @author caikang
 * @date 2016/12/08
 */
public class AliAccessToNonThreadSafeStaticFieldFromInstanceInspection
        extends AccessToNonThreadSafeStaticFieldFromInstanceInspectionBase implements
        AliBaseInspection {

    public AliAccessToNonThreadSafeStaticFieldFromInstanceInspection() {
        this.nonThreadSafeClasses.clear();
        this.nonThreadSafeClasses.add("java.text.SimpleDateFormat");
    }

    public AliAccessToNonThreadSafeStaticFieldFromInstanceInspection(@Nullable Object any) {
        this();
    }


    @Override
    public String

    ruleName()

    {
        return "AvoidCallStaticSimpleDateFormatRule";
    }

    @Override
    public String

    getDisplayName()

    {
        return RuleInspectionUtils.getRuleMessage(ruleName());
    }

    @Override
    public String buildErrorString(Object... infos)

    {
        return P3cBundle.getMessage("com.alibaba.p3c.idea.inspection.rule.AvoidCallStaticSimpleDateFormatRule.errMsg");
    }

    @Override
    public String getStaticDescription()

    {
        return RuleInspectionUtils.getRuleStaticDescription(ruleName());
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel()

    {
        return RuleInspectionUtils.getHighlightDisplayLevel(ruleName());
    }

    @Override
    public LocalQuickFix manualBuildFix(PsiElement psiElement, Boolean isOnTheFly) {
        return null;
    }

    @Override
    public PsiElement manualParsePsiElement(PsiFile psiFile, InspectionManager manager, int start, int end) {
        return psiFile.findElementAt(start);
    }
}
