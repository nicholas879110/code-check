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

import com.alibaba.p3c.idea.i18n.P3cBundle;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import kotlin.jvm.internal.Intrinsics;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 *
 *
 * @author caikang
 * @date 2017/02/28
 */
public class AvoidStartWithDollarAndUnderLineNamingQuickFix implements AliQuickFix {

    public static final String ruleName="AvoidStartWithDollarAndUnderLineNamingRule";

    @NotNull
    @Override
    public String getRuleName() {
        return ruleName;
    }

    @Override
    public boolean getOnlyOnThFly() {
        return true;
    }

    @Override
    public String getName(){
        return P3cBundle.getMessage("com.alibaba.p3c.idea.quickfix.delete_$");
    }

    @Override
    public void applyFix(Project project , ProblemDescriptor  descriptor) {
        PsiIdentifier psiIdentifier = (PsiIdentifier)(descriptor.getPsiElement()) ;
        if (psiIdentifier==null) return;
        String identifier = psiIdentifier.getText();
        String resultName = StringUtils.replacePattern(identifier, "^[\\$_]+", "");
        Long s=Long.valueOf(resultName);
        if (s != null) {
            return;
        }
        doQuickFix(resultName, project, psiIdentifier);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return groupName;
    }

    public void doQuickFix(String newIdentifier, final Project project, final PsiIdentifier psiIdentifier) {
        int offset = psiIdentifier.getTextOffset();
        if (!(psiIdentifier.getParent() instanceof PsiMember) && !(psiIdentifier.getParent() instanceof PsiLocalVariable)) {
            return;
        }

        final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;
        editor.getCaretModel().moveToOffset(psiIdentifier.getTextOffset());
        AnAction anAction = ActionManager.getInstance().getAction("RenameElement");
        final PsiFile psiFile = psiIdentifier.getContainingFile();
        commitDocumentIfNeeded(psiFile, project);
        AnActionEvent event = AnActionEvent.createFromDataContext("MainMenu", anAction.getTemplatePresentation(), new DataContext() {
            @Override
            public Object getData(String it) {
                if (Intrinsics.areEqual(it, CommonDataKeys.PROJECT.getName())){
                    return project;
                }else if (Intrinsics.areEqual(it, CommonDataKeys.EDITOR.getName())){
                    return editor;
                }else if (Intrinsics.areEqual(it, CommonDataKeys.PSI_FILE.getName())){
                    return  psiFile;
                }else if (Intrinsics.areEqual(it, CommonDataKeys.PSI_ELEMENT.getName()) ){
                    return  psiIdentifier.getParent();
                }else return null;
            }
        });
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        PsiElementFactory factory = psiFacade.getElementFactory();

        anAction.actionPerformed(event);

        // origin PsiIdentifier is unavailable
        psiFile.findElementAt(offset).replace(factory.createIdentifier(newIdentifier));
    }

    private void commitDocumentIfNeeded(PsiFile file, Project project) {
        if (file == null) {
            return;
        }
        PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
        Document cachedDocument = manager.getCachedDocument(file);
        if (cachedDocument == null) return;
        manager.commitDocument(cachedDocument);
    }



}
