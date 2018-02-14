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
package com.alibaba.p3c.idea.inspection.standalone;

;
import com.alibaba.p3c.idea.i18n.P3cBundle;
import com.alibaba.p3c.idea.inspection.AliBaseInspection;
import com.alibaba.p3c.idea.quickfix.DecorateInspectionGadgetsFix;
import com.alibaba.p3c.idea.util.HighlightDisplayLevels;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.inheritance.MissingOverrideAnnotationInspection;
import com.siyeh.ig.psiutils.MethodUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Batch QuickFix Supported
 *
 * @author caikang
 * @date 2016/12/08
 */
public class AliMissingOverrideAnnotationInspection extends MissingOverrideAnnotationInspection implements AliBaseInspection {



    private String messageKey ;

    public AliMissingOverrideAnnotationInspection() {
        this.messageKey="com.alibaba.p3c.idea.inspection.standalone.AliMissingOverrideAnnotationInspection";
    }

    public AliMissingOverrideAnnotationInspection(Object any) {
        this();
    }

//    constructor(any: Any?) : this()

    public boolean ignoreAnonymousClassMethods = false;
    public boolean ignoreObjectMethods = false;


    @NotNull
    @Override
    public String getDisplayName() {
        return P3cBundle.getMessage(this.messageKey+".message");
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return P3cBundle.getMessage(this.messageKey+".desc");
    }

    @Override
    public String ruleName() {
        return "MissingOverrideAnnotationRule";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        return P3cBundle.getMessage(this.messageKey+".errMsg");
    }

    @Override
    public JComponent createOptionsPanel() {
        return null;
    }

    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        InspectionGadgetsFix fix = super.buildFix(infos);
        if (fix == null) return null;
        return new DecorateInspectionGadgetsFix(fix, P3cBundle.getMessage("com.alibaba.p3c.idea.quickfix.standalone.AliMissingOverrideAnnotationInspection"));

    }

    @Override
    public LocalQuickFix manualBuildFix(PsiElement psiElement, Boolean isOnTheFly) {
        return buildFix(psiElement);
    }

    @NotNull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevels.BLOCKER;
    }


    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new MissingOverrideAnnotationVisitor();
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        return super.checkFile(file, manager, isOnTheFly);
    }

    private class MissingOverrideAnnotationVisitor extends BaseInspectionVisitor {
        @Override
        public void visitMethod(PsiMethod method) {
            if (method.getNameIdentifier() == null) {
                return;
            }
            if (method.isConstructor()) {
                return;
            }
            if (method.hasModifierProperty(PsiModifier.PRIVATE) || method.hasModifierProperty(PsiModifier.STATIC)) {
                return;
            }
            PsiClass methodClass = method.getContainingClass();
            if (methodClass == null) return;
            if (ignoreAnonymousClassMethods && methodClass instanceof PsiAnonymousClass) {
                return;
            }
            if (hasOverrideAnnotation(method)) {
                return;
            }
            if (!isJdk6Override(method, methodClass) && !isJdk5Override(method, methodClass)) {
                return;
            }
            if (ignoreObjectMethods && (MethodUtils.isHashCode(method) ||
                    MethodUtils.isEquals(method) ||
                    MethodUtils.isToString(method))) {
                return;
            }
            registerMethodError(method);
        }

        private Boolean hasOverrideAnnotation(PsiModifierListOwner element) {
            PsiModifierList modifierList = element.getModifierList();
            return modifierList != null ? modifierList.findAnnotation(CommonClassNames.JAVA_LANG_OVERRIDE) != null : false;
        }

        private Boolean isJdk6Override(PsiMethod method, PsiClass methodClass)

        {
            PsiMethod[] superMethods = method.findSuperMethods();
            boolean hasSupers = false;
            for (PsiMethod superMethod : superMethods) {
                PsiClass superClass = superMethod.getContainingClass();
                if (!InheritanceUtil.isInheritorOrSelf(methodClass, superClass, true)) {
                    continue;
                }
                hasSupers = true;
                if (!superMethod.hasModifierProperty(PsiModifier.PROTECTED)) {
                    return true;
                }
            }
            // is override except if this is an interface method
            // overriding a protected method in java.lang.Object
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6501053
            return hasSupers && !methodClass.isInterface();
        }

        private Boolean isJdk5Override(PsiMethod method, PsiClass methodClass)

        {
            PsiMethod[] superMethods = method.findSuperMethods();
            for (PsiMethod superMethod : superMethods) {
                PsiClass superClass = superMethod.getContainingClass();
                if (superClass == null || !InheritanceUtil.isInheritorOrSelf(methodClass, superClass, true)) {
                    continue;
                }
                if (superClass.isInterface()) {
                    continue;
                }
                if (methodClass.isInterface() && superMethod.hasModifierProperty(PsiModifier.PROTECTED)) {
                    // only true for J2SE java.lang.Object.clone(), but might
                    // be different on other/newer java platforms
                    continue;
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public PsiElement manualParsePsiElement(PsiFile psiFile, InspectionManager manager, int start, int end) {
        return psiFile.findElementAt(start);
    }
}
