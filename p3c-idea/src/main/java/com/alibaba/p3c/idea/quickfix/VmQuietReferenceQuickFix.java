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
package com.alibaba.p3c.idea.quickfix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * @author caikang
 * @date 2017/01/26
 */
public class VmQuietReferenceQuickFix implements AliQuickFix {

    public static String ruleName = "UseQuietReferenceNotationRule";

    @Override
    public boolean getOnlyOnThFly() {
        return true;
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        TextRange textRange = descriptor.getTextRangeInElement();
        if (textRange == null) return;
        Document document = FileDocumentManager.getInstance().getDocument(
                descriptor.getStartElement().getContainingFile().getVirtualFile());
        if (document == null) return;
        document.insertString(textRange.getStartOffset() + 1, "!");
    }


    @Override
    public String getName() {
        return "为变量添加!";
    }

    @NotNull
    public String getFamilyName() {
        return groupName;
    }

    @NotNull
    @Override
    public String getRuleName() {
        return ruleName;
    }
}
