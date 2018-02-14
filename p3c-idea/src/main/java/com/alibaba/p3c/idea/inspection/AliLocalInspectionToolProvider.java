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
import com.alibaba.p3c.idea.config.P3cConfig;
import com.alibaba.p3c.idea.inspection.standalone.AliAccessStaticViaInstanceInspection;
import com.alibaba.p3c.idea.inspection.standalone.AliDeprecationInspection;
import com.alibaba.p3c.idea.inspection.standalone.AliMissingOverrideAnnotationInspection;
import com.alibaba.p3c.idea.inspection.standalone.MapOrSetKeyShouldOverrideHashCodeEqualsInspection;
import com.alibaba.p3c.pmd.I18nResources;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.intellij.codeInspection.GlobalInspectionTool;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionToolsRegistrarCore;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ReflectionUtil;
import javassist.*;
import kotlin.jvm.internal.Intrinsics;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetNotFoundException;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Generated;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author caikang
 * @date 2016/12/16
 */
public class AliLocalInspectionToolProvider implements InspectionToolProvider {

    @Override
    public Class[] getInspectionClasses() {
        Class[] classes = new Class[CLASS_LIST.size()];
        classes = CLASS_LIST.toArray(classes);
        return classes;
    }


    public interface ShouldInspectChecker {
        /**
         * check inspect whether or not
         *
         * @param file file to inspect
         * @return true or false
         */
        Boolean shouldInspect(PsiFile file);
    }

    public static class RuleInfo {

        @NotNull
        private Rule rule;
        @NotNull
        private ShouldInspectChecker shouldInspectChecker;

        @NotNull
        public final Rule getRule() {
            return this.rule;
        }

        public final void setRule(@NotNull Rule var1) {
            Intrinsics.checkParameterIsNotNull(var1, "<set-?>");
            this.rule = var1;
        }

        @NotNull
        public final ShouldInspectChecker getShouldInspectChecker() {
            return this.shouldInspectChecker;
        }

        public final void setShouldInspectChecker(@NotNull AliLocalInspectionToolProvider.ShouldInspectChecker var1) {
            Intrinsics.checkParameterIsNotNull(var1, "<set-?>");
            this.shouldInspectChecker = var1;
        }

        public RuleInfo(@NotNull Rule rule, @NotNull AliLocalInspectionToolProvider.ShouldInspectChecker shouldInspectChecker) {
            Intrinsics.checkParameterIsNotNull(rule, "rule");
            Intrinsics.checkParameterIsNotNull(shouldInspectChecker, "shouldInspectChecker");
            this.rule = rule;
            this.shouldInspectChecker = shouldInspectChecker;
        }

    }

    private static Map<String, RuleInfo> ruleInfoMap = Maps.newHashMap();

    public static Map<String, RuleInfo> getRuleInfoMap() {
        return ruleInfoMap;
    }


    private static Logger LOGGER = Logger.getInstance(AliLocalInspectionToolProvider.class);
    public static List<String> ruleNames = Lists.newArrayList();
    public static List<Class<?>> CLASS_LIST = Lists.newArrayList();
    private static List<Class<?>> nativeInspectionToolClass =
            new ArrayList<Class<?>>() {{
                add(AliMissingOverrideAnnotationInspection.class);
//                add(AliAccessStaticViaInstanceInspection.class);
//                add(AliDeprecationInspection.class);
//                add(MapOrSetKeyShouldOverrideHashCodeEqualsInspection.class);
//                add(AliAccessToNonThreadSafeStaticFieldFromInstanceInspection.class);
//                add(AliArrayNamingShouldHaveBracketInspection.class);
//                add(AliControlFlowStatementWithoutBracesInspection.class);
//                add(AliEqualsAvoidNullInspection.class);
//                add(AliLongLiteralsEndingWithLowercaseLInspection.class);
//                add(AliWrapperTypeEqualityInspection.class);
            }};


    public static ShouldInspectChecker javaShouldInspectChecker = new ShouldInspectChecker() {
        @Override
        public Boolean shouldInspect(PsiFile file) {
            boolean basicInspect = (file instanceof PsiJavaFile) && (!(file instanceof PsiCompiledFile));
            if (!basicInspect) {
                return false;
            }

            if (!validScope(file)) {
                return false;
            }
            PsiElement[] psiElements = file.getChildren();
            PsiElement psiElement = null;
            for (PsiElement it : psiElements) {
                if (it instanceof PsiImportList) {
                    psiElement = it;
                    break;
                }
            }
            PsiImportList importList = (PsiImportList) psiElement;
            if (importList == null) return true;

            PsiImportStatementBase[] psiImportStatementBases = importList.getAllImportStatements();
            boolean flag = false;
            for (PsiImportStatementBase it : psiImportStatementBases) {
                if (it.getText().contains(Generated.class.getName())) {
                    flag = true;
                    break;
                }
            }

            return !flag;
        }

        private Boolean validScope(PsiFile file) {
            VirtualFile virtualFile = file.getVirtualFile();
            ProjectFileIndex index = ProjectRootManager.getInstance(file.getProject()).getFileIndex();
            return index.isInSource(virtualFile)
                    && !index.isInTestSourceContent(virtualFile)
                    && !index.isInLibraryClasses(virtualFile)
                    && !index.isInLibrarySource(virtualFile);
        }
    };

    static {
        I18nResources.changeLanguage(ServiceManager.getService(P3cConfig.class).locale);
        Thread.currentThread().setContextClassLoader(AliLocalInspectionToolProvider.class.getClassLoader());
        initPmdInspection();
        initNativeInspection();
    }

    public static void initNativeInspection() {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(DelegateLocalInspectionTool.class));
//        try {
//            for (Class<?> it : nativeInspectionToolClass) {
//                pool.insertClassPath(new ClassClassPath(it));
//                CtClass cc = pool.get(DelegateLocalInspectionTool.class.getName());
//                cc.setName("Delegate" + it.getSimpleName());
//                CtField ctField = cc.getField("forJavassist");
//                cc.removeField(ctField);
//                CtClass itClass = pool.get(it.getName());
//                CtClass toolClass = pool.get(LocalInspectionTool.class.getName());
//                CtField newField = new CtField(toolClass, "forJavassist", cc);
//                cc.addField(newField, CtField.Initializer.byNew(itClass));
//                CLASS_LIST.add(cc.toClass());
//            }
//        } catch (NotFoundException e) {
//            e.printStackTrace();
//        } catch (CannotCompileException e) {
//            e.printStackTrace();
//        }
    }

    public static void initPmdInspection() {
        for (RuleInfo ri : newRuleInfos()) {
            ruleNames.add(ri.rule.getName());
            ruleInfoMap.put(ri.rule.getName(), ri);
        }
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(DelegatePmdInspection.class));
        try {
            for (RuleInfo ruleInfo : ruleInfoMap.values()) {
                String ruleName = ruleInfo.rule.getName();
                CtClass cc = pool.get(DelegatePmdInspection.class.getName());
                cc.setName(ruleInfo.rule.getName() + "Inspection");
                CtField ctField = cc.getField("ruleName");
                cc.removeField(ctField);
                String value = "\"" + ruleInfo.rule.getName() + "\"";
                CtField newField = CtField.make("private String ruleName =" + value + ";", cc);
                cc.addField(newField, value);

                byte[] byteArr = cc.toBytecode();
                FileOutputStream fos = new FileOutputStream(new File("D://" + cc.getName() + ".class"));
                fos.write(byteArr);
                fos.close();

//                Class<?> c=cc.toClass();
//                Object obj=ReflectionUtil.newInstance(c, ArrayUtil.EMPTY_CLASS_ARRAY);
                CLASS_LIST.add(cc.toClass());
            }

        } catch (NotFoundException e) {
            LOGGER.error(e);
        } catch (CannotCompileException e) {
            LOGGER.error(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<RuleInfo> newRuleInfos() {
        List<RuleInfo> result = Lists.newArrayList();
        result.addAll(processForRuleSet("java/ali-pmd", javaShouldInspectChecker));
        result.addAll(processForRuleSet("vm/ali-other", new ShouldInspectChecker() {

            @Override
            public Boolean shouldInspect(PsiFile file) {
                VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile == null) return false;
                String path = virtualFile.getCanonicalPath();
                return path != null && path.endsWith(".vm");
            }
        }));
        return result;
    }

    private static List<RuleInfo> processForRuleSet(String ruleSetName, ShouldInspectChecker shouldInspectChecker) {
        RuleSetFactory factory = new RuleSetFactory();
        List<RuleInfo> result = Lists.newArrayList();
        try {
            RuleSet ruleSet = factory.createRuleSet(ruleSetName.replace("/", "-"));
            for (Rule it : ruleSet.getRules()) {
                result.add(new RuleInfo(it, shouldInspectChecker));
            }
        } catch (RuleSetNotFoundException e) {
            LOGGER.error(String.format("rule set %s not found for", ruleSetName));
        }

        return result;
    }

    public static void main(String[] args) {
//        AliMissingOverrideAnnotationInspection aliMissingOverrideAnnotationInspection=new AliMissingOverrideAnnotationInspection();
//        aliMissingOverrideAnnotationInspection.checkFile()
//        AliAccessStaticViaInstanceInspection aliAccessStaticViaInstanceInspection=new AliAccessStaticViaInstanceInspection();
//        aliAccessStaticViaInstanceInspection.checkFile()
//        AliLongLiteralsEndingWithLowercaseLInspection aliLongLiteralsEndingWithLowercaseLInspection=new AliLongLiteralsEndingWithLowercaseLInspection();
//        aliLongLiteralsEndingWithLowercaseLInspection.checkFile()
//        I18nResources.changeLanguage(ServiceManager.getService(P3cConfig.class).locale);
//        Thread.currentThread().setContextClassLoader(AliLocalInspectionToolProvider.class.getClassLoader());
//        initPmdInspection();
//        initNativeInspection();
//        Class[] classes = new Class[CLASS_LIST.size()];
//        classes=CLASS_LIST.toArray(classes);
//        for (final Class aClass : classes) {
//            Factory<InspectionToolWrapper> factory = new Factory<InspectionToolWrapper>() {
//                @Override
//                public InspectionToolWrapper create() {
//                    return wrapTool((InspectionProfileEntry) instantiateTool(aClass));
//                }
//            };
////            factories.add(factory);
//        }


    }


//
}
