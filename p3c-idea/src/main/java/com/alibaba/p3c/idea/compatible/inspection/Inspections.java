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
package com.alibaba.p3c.idea.compatible.inspection;

import com.alibaba.p3c.idea.inspection.AliBaseInspection;
import com.google.common.base.Splitter;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.codeInspection.javaDoc.JavaDocLocalInspection;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.Predicate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author caikang
 * @date 2017/03/01
 */
public class Inspections {
    public static List<InspectionToolWrapper<?, ?>> aliInspections(Project project) {
        InspectionProfileImpl profile = InspectionProfileService.getProjectInspectionProfile(project);
        List<InspectionToolWrapper<?, ?>> inspectionToolWrappers = getAllTools(project, profile);
        List<InspectionToolWrapper<?, ?>> filters = new ArrayList<>();
        for (InspectionToolWrapper inspectionToolWrapper : inspectionToolWrappers) {
            if (inspectionToolWrapper.getTool() instanceof AliBaseInspection) {
                filters.add(inspectionToolWrapper);
            }
        }

        return filters;
    }

    public static void addCustomTag(Project project, String tag) {
        InspectionProfileImpl profile = InspectionProfileService.getProjectInspectionProfile(project);
        JavaDocLocalInspection javaDocLocalInspection = (JavaDocLocalInspection) profile.getInspectionTool("JavaDoc", project).getTool();
        if (javaDocLocalInspection == null) return;
        if (javaDocLocalInspection.myAdditionalJavadocTags.isEmpty()) {
            javaDocLocalInspection.myAdditionalJavadocTags = tag;
            return;
        }

        List<String>  tags = Splitter.on(',').splitToList(javaDocLocalInspection.myAdditionalJavadocTags);
        if (tags.contains(tag)) {
            return;
        }
        javaDocLocalInspection.myAdditionalJavadocTags += "," + tag;
        profile.profileChanged();
        profile.scopesChanged();
    }

    private static List<InspectionToolWrapper<?, ?>> getAllTools(Project project, InspectionProfileImpl profile) {
        Method[] methods = InspectionProfileImpl.class.getMethods();
        Method method = null;
        for (Method it : methods) {
            if (it.getName().equals("getAllTools")) {
                method = it;
                break;
            }
        }
        Object result = null;
        try {
            if (method.getParameterTypes().length > 0) {
                result = method.invoke(profile, project);
            } else {
                result = method.invoke(profile);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        List<ScopeToolState> list = (List<ScopeToolState>) result;
        List<InspectionToolWrapper<?, ?>> tmp = new ArrayList<>();
        for (ScopeToolState it : list) {
            tmp.add(it.getTool());
        }
        return tmp;
    }
}
