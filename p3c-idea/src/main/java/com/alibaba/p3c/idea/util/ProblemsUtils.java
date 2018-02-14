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
package com.alibaba.p3c.idea.util;

;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.ElementType;
import kotlin.jvm.internal.Intrinsics;

import java.util.Collections;
import java.util.Set;

/**
 * @author caikang
 * @date 2017/03/16
 * 6
 */
public class ProblemsUtils {
    private static Set<String> highlightLineRules = Collections.singleton("AvoidCommentBehindStatement");

    public static ProblemDescriptor createProblemDescriptorForPmdRule(PsiFile psiFile, InspectionManager manager, Boolean isOnTheFly,
                                                                      String ruleName, String desc, int start, int end,
                                                                      int checkLine
                                                                    /*  quickFix:(PsiElement) ->LocalQuickFix?=

    {
        QuickFixes.getQuickFix(ruleName, isOnTheFly)
    }*/)

    {
        Document document = FileDocumentManager.getInstance().getDocument(psiFile.getVirtualFile());
        if (document == null) return null;
        if (highlightLineRules.contains(ruleName) && checkLine <= document.getLineCount()) {
            int lineNumber = 0;
            if (start >= document.getTextLength()) {
                lineNumber = document.getLineCount() - 1;
            } else {
                lineNumber = document.getLineNumber(start);
            }
            TextRange textRange = new TextRange(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber));
            return createTextRangeProblem(manager, textRange, isOnTheFly, psiFile, ruleName, desc);
        }
        if (psiFile.getVirtualFile().getCanonicalPath() != null && psiFile.getVirtualFile().getCanonicalPath().endsWith(".vm")) {
            return createTextRangeProblem(manager, new TextRange(start, end), isOnTheFly, psiFile, ruleName, desc);
        }
        PsiElement psiElement = psiFile.findElementAt(start);
        if (psiElement == null) return null;
        psiElement = transform(psiElement);
        if (psiElement == null) return null;
        PsiElement endElement = null;
        if (start == end) {
            endElement = psiElement;
        } else {
            endElement = getEndElement(psiFile, psiElement, end);
        }
        if (psiElement != endElement && endElement.getParent() instanceof PsiField) {
            psiElement = endElement;
        }
        if (endElement instanceof PsiWhiteSpace) {
            endElement = psiElement;
        }
        if (psiElement instanceof PsiWhiteSpace) {
            TextRange textRange = new TextRange(start, end);
            return createTextRangeProblem(manager, textRange, isOnTheFly, psiFile, ruleName, desc);
        }

        if (psiElement.getTextRange().getStartOffset() >= endElement.getTextRange().getEndOffset()) {
            if (!(psiElement instanceof PsiFile &&endElement instanceof PsiFile)){
                return null;
            }
            endElement = psiElement;
        }
        Intrinsics.checkParameterIsNotNull(psiElement, "psiElement");
        return manager.createProblemDescriptor(psiElement, endElement,
                desc, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
                quickFix(ruleName,isOnTheFly));
    }

    private static LocalQuickFix quickFix(String ruleName, Boolean isOnTheFly) {
        return QuickFixes.getQuickFix(ruleName, isOnTheFly);
    }


    private static PsiElement getEndElement(PsiFile psiFile, PsiElement psiElement, int endOffset) {
        PsiElement endElement = psiFile.findElementAt(endOffset);
        if (endElement instanceof PsiJavaToken && ((PsiJavaToken) endElement).getTokenType() == ElementType.SEMICOLON) {
            endElement = psiFile.findElementAt(endOffset - 1);
        }
        if (endElement instanceof PsiIdentifier) {
            return endElement;
        }
        if (psiElement instanceof PsiIdentifier) {
            return psiElement;
        }
        if (endElement == null || endElement instanceof PsiWhiteSpace
                || psiElement.getTextRange().getStartOffset() >= endElement.getTextRange().getEndOffset()) {
            endElement = psiElement;
        }
        return endElement;
    }

    private static PsiElement transform(PsiElement element) {
        PsiElement psiElement = element;
        while (psiElement instanceof PsiWhiteSpace) {
            psiElement = psiElement.getNextSibling();
        }
        if (psiElement == null) {
            return null;
        }
        if (psiElement instanceof PsiKeyword && psiElement.getText() != null && (ObjectConstants.CLASS_LITERAL == psiElement.getText()
                || ObjectConstants.INTERFACE_LITERAL == psiElement.getText()
                || ObjectConstants.ENUM_LITERAL == psiElement.getText()) && psiElement.getParent() instanceof PsiClass) {
            PsiClass parent = (PsiClass) (psiElement.getParent());
            PsiIdentifier identifier = parent.getNameIdentifier();
            return identifier != null ? (PsiElement) identifier : psiElement;

        }
        return psiElement;
    }

    private static ProblemDescriptor createTextRangeProblem(InspectionManager manager, TextRange textRange, Boolean isOnTheFly,
                                                     PsiFile psiFile, String ruleName, String desc
                                      /* quickFix:() ->LocalQuickFix?=

    {

    }*/)

    {
        QuickFixes.getQuickFix(ruleName, isOnTheFly);
        return manager.createProblemDescriptor(psiFile, textRange,
                desc, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                isOnTheFly, quickFix(ruleName,isOnTheFly));
    }



}
