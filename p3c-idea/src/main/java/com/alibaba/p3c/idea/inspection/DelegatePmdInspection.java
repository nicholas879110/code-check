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
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author caikang
 * @date 2017/02/28
 */
public class DelegatePmdInspection extends LocalInspectionTool implements AliBaseInspection, PmdRuleInspectionIdentify {

    private String ruleName;
    private AliPmdInspection aliPmdInspection;

    public boolean runForWholeFile() {
        return this.aliPmdInspection.runForWholeFile();
    }

    @Nullable
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        Intrinsics.checkParameterIsNotNull(file, "file");
        Intrinsics.checkParameterIsNotNull(manager, "manager");
        return this.aliPmdInspection.checkFile(file, manager, isOnTheFly);
    }

    @Nullable
    public String getStaticDescription() {
        return this.aliPmdInspection.getStaticDescription();
    }

    @NotNull
    public String ruleName() {
        String var10000 = this.ruleName;
        if (this.ruleName == null) {
            Intrinsics.throwNpe();
        }

        return var10000;
    }

    @Nls
    @NotNull
    public String getDisplayName() {
        return this.aliPmdInspection.getDisplayName();
    }

    @NotNull
    public HighlightDisplayLevel getDefaultLevel() {
        return this.aliPmdInspection.getDefaultLevel();
    }

    @Nls
    @NotNull
    public String getGroupDisplayName() {
        return this.aliPmdInspection.getGroupDisplayName();
    }

    public boolean isEnabledByDefault() {
        return this.aliPmdInspection.isEnabledByDefault();
    }

    @NotNull
    public String getShortName() {
        return this.aliPmdInspection.getShortName();
    }

    public boolean isSuppressedFor(@NotNull PsiElement element) {
        Intrinsics.checkParameterIsNotNull(element, "element");
        return false;
    }

    public DelegatePmdInspection() {
        if (this.ruleName == null) {
            Intrinsics.throwNpe();
        }
        AliPmdInspection var10001 = new AliPmdInspection(this.ruleName);
        this.aliPmdInspection = var10001;
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
