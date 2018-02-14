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
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocToken;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author caikang
 * @date 2017/02/27
 */
public class ClassMustHaveAuthorQuickFix extends InspectionGadgetsFix implements AliQuickFix {

    public static final String tag = "@author " + System.getProperty("user.name") == null ? System.getenv("USER") : System.getProperty("user.name");

    public static String ruleName = "ClassMustHaveAuthorRule";

    public void doFix(Project project, ProblemDescriptor descriptor) {
        if (descriptor == null) return;
        PsiElement psiElement = descriptor.getPsiElement();
        if (!(psiElement instanceof PsiClass)) {
            psiElement = null;
        }
        PsiClass psiClass = (PsiClass) psiElement;
        if (psiClass == null) {
            if (psiElement == null) {
                psiClass = null;
            } else {
                if (!(psiElement.getParent() instanceof PsiClass)) {
                    psiClass = null;
                } else {
                    psiClass = (PsiClass) (psiElement.getParent());
                }
            }
        }
        if (psiClass == null) return;

//        val psiClass = descriptor.psiElement as? PsiClass ?: descriptor.psiElement?.parent as? PsiClass ?: return

        PsiDocComment document = psiClass.getDocComment();
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        PsiElementFactory factory = psiFacade.getElementFactory();
        if (document == null) {
            PsiDocComment doc = factory.createDocCommentFromText("\n/**\n * " + tag + "\n */\n");
            if (psiClass.isEnum()) {
                psiClass.getContainingFile().addAfter(doc, psiClass.getPrevSibling());
            } else {
                psiClass.addBefore(doc, psiClass.getFirstChild());
            }
            return;
        }

        for (PsiElement line : document.getDescriptionElements()) {
            Pattern pattern = Pattern.compile("Created by (.*) on (.*)\\.");
            Matcher matcher = pattern.matcher(line.getText());
            if (line instanceof PsiDocToken && matcher.find()) {
                List<String> groups = new ArrayList<>();
                while (matcher.find()) {
                    groups.add(matcher.group());
                }
                if (groups.size() == 0) continue;
//                val groups = matcher.groups ?:continue
                String author = groups.get(1);
                if (author == null) continue;
                String date = groups.get(2);
                if (date == null) continue;
                document.addBefore(factory.createDocTagFromText("@date $date"), line);
                document.addBefore(factory.createDocTagFromText("@author $author"), line);
                line.delete();
                return;
            }
        }

        if (document.getTags() != null && document.getTags().length > 0) {
            document.addBefore(factory.createDocTagFromText(tag), document.getTags()[0]);
            return;
        }

        document.add(factory.createDocTagFromText(tag));
    }

    @NotNull
    public String getRuleName() {
        return ruleName;
    }

    public boolean getOnlyOnThFly() {
        return true;
    }

    @Override
    public String getName() {
        return P3cBundle.getMessage("com.alibaba.p3c.idea.quickfix.generate.author");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return groupName;
    }
}
