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
package com.alibaba.p3c.idea.action;

import com.alibaba.p3c.idea.compatible.inspection.InspectionProfileService;
import com.alibaba.p3c.idea.compatible.inspection.Inspections;
import com.alibaba.p3c.idea.ep.InspectionActionExtensionPoint;
import com.alibaba.p3c.idea.i18n.P3cBundle;
import com.alibaba.p3c.pmd.lang.java.util.NumberConstants;
import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author caikang
 * @date 2016/12/11
 */
public class AliInspectionAction extends AnAction {


    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        AnalysisUIOptions analysisUIOptions = ServiceManager.getService(project, AnalysisUIOptions.class);
        if (analysisUIOptions == null) {
            throw new RuntimeException("analysisUIOptions is null!");
        }
        analysisUIOptions.GROUP_BY_SEVERITY = true;
        InspectionManagerEx managerEx = (InspectionManagerEx) InspectionManager.getInstance(project);
        List<InspectionToolWrapper<?, ?>> toolWrappers = Inspections.aliInspections(project);
        PsiElement psiElement = e.getData(DataKeys.PSI_ELEMENT);
        PsiFile psiFile = e.getData(DataKeys.PSI_FILE);
        VirtualFile virtualFile = e.getData(DataKeys.VIRTUAL_FILE);
        VirtualFile[] virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        AnalysisScope analysisScope = null;
        boolean projectDir = false;
        if (psiFile != null) {
            analysisScope = new AnalysisScope(psiFile);
            projectDir = isBaseDir(psiFile.getVirtualFile(), project);
        } else if (virtualFiles != null && virtualFiles.length > NumberConstants.INTEGER_SIZE_OR_LENGTH_0) {
            analysisScope = new AnalysisScope(project, new ArrayList<VirtualFile>(Arrays.asList(virtualFiles)));
            for (VirtualFile it : virtualFiles) {
                if (isBaseDir(it, project)) {
                    projectDir = true;
                    break;
                }
            }
        } else {
            if (virtualFile != null && virtualFile.isDirectory()) {
                PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile);
                if (psiDirectory != null) {
                    analysisScope = new AnalysisScope(psiDirectory);
                    projectDir = isBaseDir(virtualFile, project);
                }
            }
            if (analysisScope == null && virtualFile != null) {
                analysisScope = new AnalysisScope(project, Collections.singletonList(virtualFile));
                projectDir = isBaseDir(virtualFile, project);
            }
            if (analysisScope == null) {
                projectDir = true;
                analysisScope = new AnalysisScope(project);
            }
        }
        if (e.getInputEvent() instanceof KeyEvent) {
            inspectForKeyEvent(project, managerEx, toolWrappers, psiElement, psiFile, virtualFile, analysisScope);
            return;
        }
        PsiElement element = psiFile!=null ?psiFile: psiElement;
        analysisScope.setIncludeTestSource(false)  ;
        analysisScope.setSearchInLibraries(true);
        createContext(toolWrappers, managerEx, element,
                projectDir).doInspections(analysisScope);
    }

    private Boolean isBaseDir(VirtualFile file, Project project) {
        if (file.getCanonicalPath() == null || project.getBasePath() == null) {
            return false;
        }
        return project.getBasePath() == file.getCanonicalPath();
    }

    private void inspectForKeyEvent(Project project, InspectionManagerEx managerEx,
                                    List<InspectionToolWrapper<?, ?>> toolWrappers, PsiElement psiElement, PsiFile psiFile,
                                    VirtualFile virtualFile, AnalysisScope analysisScope) {
        Module module = null;
        if (virtualFile != null && project.getBaseDir() != virtualFile) {
            module = ModuleUtilCore.findModuleForFile(virtualFile, project);
        }

        AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
        uiOptions.ANALYZE_TEST_SOURCES = false;
        BaseAnalysisActionDialog dialog = new BaseAnalysisActionDialog("Select Analyze Scope", "Analyze Scope", project, analysisScope,
                module != null ? module.getName() : null, true, uiOptions, psiElement);

        if (!dialog.showAndGet()) {
            return;
        }
        AnalysisScope scope = dialog.getScope(uiOptions, analysisScope, project, module);
        scope.setSearchInLibraries(true);
        PsiElement element = psiFile != null ? psiFile : psiElement;
        createContext(toolWrappers, managerEx, element, dialog.isProjectScopeSelected()).doInspections(scope);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setText(P3cBundle.getMessage("com.alibaba.p3c.idea.action.AliInspectionAction.text"));
    }

    public static Logger logger = Logger.getInstance(AliInspectionAction.class);

    public static GlobalInspectionContextImpl createContext(List<InspectionToolWrapper<?, ?>> toolWrapperList,
                                                     InspectionManagerEx managerEx, PsiElement psiElement, Boolean projectScopeSelected) {
        InspectionProfileImpl model = InspectionProfileService.createSimpleProfile(toolWrapperList, managerEx, psiElement);
        GlobalInspectionContextImpl inspectionContext = createNewGlobalContext(managerEx, projectScopeSelected);
        InspectionProfileService.setExternalProfile(model, inspectionContext);
        return inspectionContext;
    }


    private static GlobalInspectionContextImpl createNewGlobalContext(InspectionManagerEx managerEx,
                                                               final Boolean projectScopeSelected) {
        return new GlobalInspectionContextImpl(managerEx.getProject(), managerEx.getContentManager()) {


            @Override
            protected void runTools(@NotNull AnalysisScope scope, boolean runGlobalToolsOnly, boolean isOfflineInspections) {
                super.runTools(scope, runGlobalToolsOnly, isOfflineInspections);
                if (myProgressIndicator.isCanceled()) {
                    return;
                }
                InspectionActionExtensionPoint[] extensionPoints = InspectionActionExtensionPoint.extension.getExtensions();
                for (InspectionActionExtensionPoint it : extensionPoints) {
                    try {
                        it.doOnInspectionFinished(this, projectScopeSelected);
                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            }

            @Override
            public void close(final boolean noSuspisiousCodeFound) {
                super.close(noSuspisiousCodeFound);
                InspectionActionExtensionPoint[] extensionPoints = InspectionActionExtensionPoint.extension.getExtensions();
                for (InspectionActionExtensionPoint it : extensionPoints) {
                    try {
                        it.doOnClose(noSuspisiousCodeFound, getProject());
                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            }

            @Override
            public void addView(@NotNull InspectionResultsView view) {
                super.addView(view);
                InspectionActionExtensionPoint[] extensionPoints = InspectionActionExtensionPoint.extension.getExtensions();
                for (InspectionActionExtensionPoint it : extensionPoints) {
                    try {
                        it.doOnView(view);
                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            }
        };
    }


}
