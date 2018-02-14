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
import com.alibaba.p3c.idea.util.NumberConstants;
import com.alibaba.p3c.idea.util.ObjectConstants;
import com.alibaba.p3c.idea.i18n.P3cBundle;
import com.alibaba.p3c.idea.inspection.AliBaseInspection;
import com.alibaba.p3c.idea.util.HighlightDisplayLevels;
import com.beust.jcommander.internal.Sets;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.psi.*;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author caikang
 * @date 2017/03/01
 */
public class MapOrSetKeyShouldOverrideHashCodeEqualsInspection extends BaseInspection implements AliBaseInspection {

    public String messageKey = "com.alibaba.p3c.idea.inspection.standalone.MapOrSetKeyShouldOverrideHashCodeEqualsInspection";

    public MapOrSetKeyShouldOverrideHashCodeEqualsInspection() {
        this.messageKey = "com.alibaba.p3c.idea.inspection.standalone.MapOrSetKeyShouldOverrideHashCodeEqualsInspection";
    }

    public MapOrSetKeyShouldOverrideHashCodeEqualsInspection(@Nullable Object any) {
        this();
    }

    @Override
    public String getDisplayName() {
        return P3cBundle.getMessage(this.messageKey+".message");
    }

    @Override
    public String getStaticDescription() {
        return P3cBundle.getMessage(this.messageKey+".desc");
    }

    @Override
    public String buildErrorString(Object... infos) {
        PsiClassType type = (PsiClassType) infos[0];
        return String.format(P3cBundle.getMessage(this.messageKey+".errMsg"), type.getClassName());
    }



    @Override
    public BaseInspectionVisitor buildVisitor(){
        return new MapOrSetKeyVisitor();
    }

    @Override
    public String ruleName() {
        return "MapOrSetKeyShouldOverrideHashCodeEqualsRule";
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevels.CRITICAL;
    }

    public enum ClassType {
        /**
         * parameter type is Set
         */
        SET,
        MAP, OTHER;

        @Override
        public String toString()

        {
            String string = super.toString();
            return string.substring(0, 1) + string.substring(1).toLowerCase();
        }
    }

    private class MapOrSetKeyVisitor extends BaseInspectionVisitor {

        private ClassType getClassType(PsiClass aClass) {
            Set<PsiClass> classSet = Sets.newHashSet();
            return isMapOrSet(aClass, classSet);
        }

        private ClassType isMapOrSet(PsiClass aClass, Set<PsiClass> visitedClasses) {
            if (aClass == null) {
                return ClassType.OTHER;
            }
            if (!visitedClasses.add(aClass)) {
                return ClassType.OTHER;
            }
            @NonNls
            String className = aClass.getQualifiedName();
            if (Intrinsics.areEqual(CommonClassNames.JAVA_UTIL_SET,className)) {
                return ClassType.SET;
            }
            if (Intrinsics.areEqual(CommonClassNames.JAVA_UTIL_MAP,className)) {
                return ClassType.MAP;
            }
            PsiClass[] supers = aClass.getSupers();
            List<ClassType> classTypes = new ArrayList<>(supers.length);
            for (PsiClass it : supers) {
                ClassType classType = isMapOrSet(it, visitedClasses);
                classTypes.add(classType);
            }


            ClassType classTypeDes = null;
            for (ClassType classType : classTypes) {
                if (!Intrinsics.areEqual(classType,ClassType.OTHER)) {
                    continue;
                }
                classTypeDes = classType;
                break;
            }

            if (classTypeDes == null) {
                classTypeDes = ClassType.OTHER;
            }

//            return supers.map {isMapOrSet(it, visitedClasses)}.firstOrNull { it != ClassType.OTHER}?:ClassType.OTHER
            return classTypeDes;
        }

        @Override
        public void visitVariable(PsiVariable variable) {
            super.visitVariable(variable);
            PsiTypeElement typeElement = variable.getTypeElement();
            if (typeElement == null) return;
            PsiType psiType=typeElement.getType();
            if (!(psiType instanceof PsiClassType)) {
                psiType = null;
            }
            PsiClassType type = (PsiClassType) psiType;
            if (type == null) return;
            PsiJavaCodeReferenceElement referenceElement = typeElement.getInnermostComponentReferenceElement();
            if (referenceElement == null) return;
            PsiClass aClass = type.resolve();

            ClassType collectionType = getClassType(aClass);
            if (Intrinsics.areEqual(collectionType ,ClassType.OTHER)) {
                return;
            }
            PsiReferenceParameterList parameterList = referenceElement.getParameterList();
            if (parameterList == null || parameterList.getTypeParameterElements().length == NumberConstants.INTEGER_SIZE_OR_LENGTH_0) {
                return;
            }
            PsiType papsiType = parameterList.getTypeArguments()[0];
            if (!redefineHashCodeEquals(papsiType)) {
                registerError(parameterList.getTypeParameterElements()[0], papsiType);
            }
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression expression) {
            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
            if (qualifierExpression==null)return;
            PsiClassType type = qualifierExpression.getType() == null ? null : (PsiClassType) qualifierExpression.getType();
            if (type == null) return;
            PsiClass aClass = type.resolve();

            ClassType collectionType = getClassType(aClass);
            if (Intrinsics.areEqual(collectionType , ClassType.OTHER) ){
                return;
            }
            @NonNls
            String methodName = methodExpression.getReferenceName();
            if (Intrinsics.areEqual(collectionType ,ClassType.SET) && !Intrinsics.areEqual(ObjectConstants.METHOD_NAME_ADD , methodName)) {
                return;
            }
            if (Intrinsics.areEqual(collectionType, ClassType.MAP) && !Intrinsics.areEqual(ObjectConstants.METHOD_NAME_PUT,methodName)) {
                return;
            }
            PsiExpressionList argumentList = expression.getArgumentList();
            PsiExpression[]  arguments = argumentList.getExpressions();
            if (Intrinsics.areEqual(collectionType ,ClassType.SET) && arguments.length != NumberConstants.INTEGER_SIZE_OR_LENGTH_1) {
                return;
            }
            if (Intrinsics.areEqual(collectionType ,ClassType.MAP) && arguments.length != NumberConstants.INTEGER_SIZE_OR_LENGTH_2) {
                return;
            }
            PsiExpression argument = arguments[0];
            PsiType argumentType = argument.getType();
            if (argumentType == null || redefineHashCodeEquals(argumentType)) {
                return;
            }
            registerMethodCallError(expression, argumentType);
        }
    }


    private static Boolean redefineHashCodeEquals(PsiType psiType) {
        if (!(psiType instanceof PsiClassType)) {
            return true;
        }
        PsiClass psiClass = ((PsiClassType) psiType).resolve();
        if (psiClass == null || psiClass.getContainingFile() == null || psiClass instanceof PsiTypeParameter
                || psiClass.isEnum() || psiClass.isInterface()) {
            return true;
        }
        if (!(psiClass.getContainingFile().getFileType() instanceof JavaFileType)) {
            return true;
        }
        PsiMethod[] hashCodeMethods = psiClass.findMethodsByName(ObjectConstants.METHOD_NAME_HASHCODE, false);
        if (hashCodeMethods.length == NumberConstants.INTEGER_SIZE_OR_LENGTH_0) {
            return false;
        }
        PsiMethod[] equalsMethods = psiClass.findMethodsByName(ObjectConstants.METHOD_NAME_EQUALS, false);
        return equalsMethods.length > NumberConstants.INTEGER_SIZE_OR_LENGTH_0;
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
