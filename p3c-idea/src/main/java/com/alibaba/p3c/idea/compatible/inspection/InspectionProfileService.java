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

;
import com.alibaba.smartfox.idea.common.util.PluginVersions;
import com.google.common.collect.Sets;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.PrimitiveConvertor;
import org.jdom.Element;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author caikang
 * @date 2017/03/01
 */
public class InspectionProfileService {
    public static InspectionProfileImpl createSimpleProfile(List<InspectionToolWrapper<?, ?>> toolWrapperList,
                                                            InspectionManagerEx managerEx, PsiElement psiElement) {
        InspectionProfileImpl profile = getProjectInspectionProfile(managerEx.getProject());
        LinkedHashSet<InspectionToolWrapper> allWrappers = Sets.newLinkedHashSet();
        allWrappers.addAll(toolWrapperList);
        LinkedHashSet<InspectionToolWrapper> forCompile = allWrappers;
        for (InspectionToolWrapper<?, ?> toolWrapper : allWrappers) {
            profile.collectDependentInspections(toolWrapper, forCompile, managerEx.getProject());
        }
        InspectionProfileImpl model = null;
        if (PluginVersions.baseVersion171 <= PluginVersions.getBaseVersion() && PluginVersions.getBaseVersion() <= 2147483647) {
            try {
                Class clz = Class.forName("com.intellij.codeInspection.ex.InspectionProfileKt");
                Method[] methods = clz.getMethods();
                Method method = null;
                for (Method it : methods) {
                    if (it.getName().equals("createSimple")) {
                        method = it;
                        break;
                    }
                }
                List<InspectionToolWrapper> list = new ArrayList<>();
                Iterator<InspectionToolWrapper> iterator = allWrappers.iterator();
                while (iterator.hasNext()) {
                    list.add(iterator.next());
                }
                model = (InspectionProfileImpl) method.invoke(null, "Alibaba Coding Guidelines", managerEx.getProject(), list);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else if (PluginVersions.getBaseVersion() == PluginVersions.baseVersion163) {

            try {
                Method[] methods = profile.getClass().getMethods();
                Method method = null;
                for (Method it : methods) {
                    if (it.getName().equals("createSimple")) {
                        method = it;
                        break;
                    }
                }
                List<InspectionToolWrapper> list = new ArrayList<>();
                Iterator<InspectionToolWrapper> iterator = allWrappers.iterator();
                while (iterator.hasNext()) {
                    list.add(iterator.next());
                }
                model = (InspectionProfileImpl) method.invoke(null, "Alibaba Coding Guidelines", managerEx.getProject(), list);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        } else {
            try {
                Method[] methods = profile.getClass().getMethods();
                Method method = null;
                for (Method it : methods) {
                    if (it.getName().equals("createSimple")) {
                        method = it;
                        break;
                    }
                }
                InspectionToolWrapper[] wrappers = new InspectionToolWrapper[allWrappers.size()];
                wrappers=allWrappers.toArray(wrappers);
                model = (InspectionProfileImpl) method.invoke(null, "Alibaba Coding Guidelines", managerEx.getProject(),wrappers );
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }
        try {
            Element element = new Element("toCopy");
            for (InspectionToolWrapper wrapper : allWrappers) {
                wrapper.getTool().writeSettings(element);
                InspectionToolWrapper tw = null;
                if (psiElement == null) {
                    tw = model.getInspectionTool(wrapper.getShortName(), managerEx.getProject());
                } else {
                    tw = model.getInspectionTool(wrapper.getShortName(), psiElement);
                }
                tw.getTool().readSettings(element);
            }
        } catch (WriteExternalException e) {
        } catch (InvalidDataException e) {
        }
        return model;
    }

    public void toggleInspection(Project project, List<InspectionToolWrapper<?, ?>> aliInspections, Boolean closed) {
        InspectionProfileImpl profile = getProjectInspectionProfile(project);
        List<String> shortNames = new ArrayList<>();
        for (InspectionToolWrapper it : aliInspections) {
            shortNames.add(it.getTool().getShortName());
        }
        profile.removeScopes(shortNames, "AlibabaCodeAnalysis", project);
        Method[] methods = profile.getClass().getMethods();
        Method method = null;
        for (Method it : methods) {
            if (it.getName().equals(closed ? "enableToolsByDefault" : "disableToolByDefault")) {
                method = it;
                break;
            }
        }
        try {
            method.invoke(profile, shortNames, project);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        profile.profileChanged();
        profile.scopesChanged();
    }

    public static void setExternalProfile(InspectionProfileImpl profile, GlobalInspectionContextImpl inspectionContext) {
        Method[] methods = inspectionContext.getClass().getMethods();
        Method method = null;
        for (Method it : methods) {
            if (it.getName().equals("setExternalProfile") && it.getParameterTypes().length == 1 && it.getParameterTypes()[0].isAssignableFrom(InspectionProfileImpl.class)) {
                method = it;
                break;
            }
        }
        if (method != null) {
            try {
                method.invoke(inspectionContext, profile);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public static InspectionProfileImpl getProjectInspectionProfile(Project project) {
        return (InspectionProfileImpl) InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    }
}
