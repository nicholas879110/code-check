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
;
import com.alibaba.p3c.idea.i18n.P3cBundle;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author caikang
 * @date 2017/02/28
 */
public class LowerCamelCaseVariableNamingQuickFix implements AliQuickFix {

    public static final String ruleName="LowerCamelCaseVariableNamingRule";

    @NotNull
    public String getRuleName() {
        return ruleName;
    }

    public boolean getOnlyOnThFly() {
        return true;
    }

    @NotNull
    public String getName() {
        return P3cBundle.getMessage("com.alibaba.p3c.idea.quickfix.variable.lowerCamelCase");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return groupName;
    }

    @Override
    public  void  applyFix(Project project , ProblemDescriptor descriptor ) {
        PsiIdentifier psiIdentifier = (PsiIdentifier)(descriptor.getPsiElement() );
        if (psiIdentifier==null) return;
        String identifier = psiIdentifier.getText();
        String resultName = toLowerCamelCase(identifier);
        doQuickFix(resultName, project, psiIdentifier);
    }

    private String toLowerCamelCase(String identifier )  {
        List<String> list = Splitter.onPattern("[^a-z0-9A-Z]+").trimResults().omitEmptyStrings().splitToList(identifier);
        List<String> result=new ArrayList<>();
        for (int i=0;i<list.size();i++){
            String s=list.get(i);
            if (i==0){
                result.add(s.toLowerCase());
            }else{
                char[] charArray = s.toLowerCase().toCharArray();
                charArray[0] = Character.toUpperCase(charArray[0]);
                result.add( new String(charArray));
            }
        }
        return Joiner.on("").join(result);
    }

    public void doQuickFix(String newIdentifier,final Project project, final PsiIdentifier psiIdentifier) {
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
