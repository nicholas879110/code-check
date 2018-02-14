package com.alibaba.p3c.idea.inspection;

import com.alibaba.p3c.idea.config.P3cConfig;
import com.alibaba.p3c.pmd.I18nResources;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

public class TEst {

    public static void main(String[] args) {
        System.out.println("---");
//        I18nResources.changeLanguage(ServiceManager.getService(P3cConfig.class).locale);
//        Thread.currentThread().setContextClassLoader(AliLocalInspectionToolProvider.class.getClassLoader());
        AliLocalInspectionToolProvider.initPmdInspection();
        AliLocalInspectionToolProvider.initNativeInspection();
        Class[] classes = new Class[AliLocalInspectionToolProvider.CLASS_LIST.size()];
        classes=AliLocalInspectionToolProvider.CLASS_LIST.toArray(classes);
        for (final Class aClass : classes) {
           Object obj= instantiateTool(aClass);
        }

    }

    static Object instantiateTool(@NotNull Class<?> toolClass) {
        try {
            return ReflectionUtil.newInstance(toolClass, ArrayUtil.EMPTY_CLASS_ARRAY);
        }
        catch (RuntimeException e) {
           e.printStackTrace();
        }

        return null;
    }

//    public static InspectionToolWrapper wrapTool(@NotNull InspectionProfileEntry profileEntry) {
//        if (profileEntry instanceof LocalInspectionTool) {
//            return new LocalInspectionToolWrapper((LocalInspectionTool)profileEntry);
//        }
//        if (profileEntry instanceof GlobalInspectionTool) {
//            return new GlobalInspectionToolWrapper((GlobalInspectionTool)profileEntry);
//        }
//        throw new RuntimeException("unknown inspection class: " + profileEntry + "; "+profileEntry.getClass());
//    }
}
