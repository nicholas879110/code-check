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
package com.alibaba.p3c.idea.vcs;

;
import com.alibaba.p3c.idea.action.AliInspectionAction;
import com.alibaba.p3c.idea.compatible.inspection.Inspections;
import com.alibaba.p3c.idea.config.P3cConfig;
import com.alibaba.p3c.idea.inspection.AliBaseInspection;
import com.alibaba.smartfox.idea.common.util.BalloonNotifications;
import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerUtil;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.PairConsumer;
import com.intellij.vcsUtil.Rethrow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * @author yaohui.wyh
 * @author caikang
 * @date 2017/03/21
 * @date 2017/05/04
 */
public class AliCodeAnalysisCheckinHandler extends CheckinHandler {

    private Project myProject;
    private CheckinProjectPanel myCheckinPanel;

    AliCodeAnalysisCheckinHandler(Project myProject, CheckinProjectPanel myCheckinPanel) {
        this.myProject = myProject;
        this.myCheckinPanel = myCheckinPanel;

    }

    private String dialogTitle = "Alibaba Code Analyze";
    private String cancelText = "&Cancel";
    private String commitText = "&Commit Anyway";
    private String waitingText = "Wait";


    Logger log = Logger.getInstance(AliCodeAnalysisCheckinHandler.class);

    @Nullable
    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final NonFocusableCheckBox checkBox = new NonFocusableCheckBox("Alibaba Code Guidelines");
        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(checkBox);
                boolean dumb = DumbService.isDumb(myProject);
                checkBox.setEnabled(!dumb);
                checkBox.setToolTipText(dumb ? "Code analysis is impossible until indices are up-to-date" : "");
                return panel;
            }

            @Override
            public void refresh() {

            }

            @Override
            public void saveState() {

            }

            @Override
            public void restoreState() {
                checkBox.setSelected(getSettings().analysisBeforeCheckin);
            }
        };
    }


    private P3cConfig getSettings() {
        return ServiceManager.getService(P3cConfig.class);
    }

    @Override
    public CheckinHandler.ReturnResult beforeCheckin(CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
        if (!getSettings().analysisBeforeCheckin) {
            return CheckinHandler.ReturnResult.COMMIT;
        }
        if (DumbService.getInstance(myProject).isDumb()) {
            if (Messages.showOkCancelDialog(myProject,
                    "Code analysis is impossible until indices are up-to-date", dialogTitle,
                    waitingText, commitText, null) == Messages.OK) {
                return CheckinHandler.ReturnResult.CANCEL;
            }
            return CheckinHandler.ReturnResult.COMMIT;
        }

        List<VirtualFile> virtualFiles = CheckinHandlerUtil.filterOutGeneratedAndExcludedFiles(myCheckinPanel.getVirtualFiles(), myProject);
        Boolean hasViolation = hasViolation(virtualFiles, myProject);
        if (!hasViolation) {
            BalloonNotifications.showSuccessNotification("No suspicious code found！", myProject, "Analyze Finished", false);
            return CheckinHandler.ReturnResult.COMMIT;
        }
        if (Messages.showOkCancelDialog(myProject, "Found suspicious code,continue commit？",
                dialogTitle, commitText, cancelText, null) == Messages.OK) {
            return CheckinHandler.ReturnResult.COMMIT;
        } else {
            VirtualFile[] files=new VirtualFile[virtualFiles.size()];
            doAnalysis(myProject, virtualFiles.toArray(files));
            return CheckinHandler.ReturnResult.CLOSE_WINDOW;
        }
    }

    public void doAnalysis(Project project, VirtualFile[] virtualFiles) {
        InspectionManagerEx managerEx = (InspectionManagerEx) InspectionManager.getInstance(project);
        AnalysisScope analysisScope = new AnalysisScope(project, new ArrayList(Arrays.asList(virtualFiles)));
        List<InspectionToolWrapper<?, ?>> tools = Inspections.aliInspections(project);

//        val tools = Inspections.aliInspections(project) { it.tool is AliBaseInspection }
        AliInspectionAction.createContext(tools, managerEx, null, false)
                .doInspections(analysisScope);
    }

    private Boolean hasViolation(final List<VirtualFile> virtualFiles, final Project project) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        if (ApplicationManager.getApplication().isWriteAccessAllowed())
            throw new RuntimeException("Must not run under write action");
        final AtomicBoolean result = new AtomicBoolean(false);
        final Ref<Exception> exception = Ref.create();
        ProgressManager.getInstance().run(
                new Task.Modal(myProject, VcsBundle.message("checking.code.smells.progress.title"), true) {

                    @Override
                    public void run(@NotNull ProgressIndicator progress) {
                        try {
                            List<InspectionToolWrapper<?, ?>> tools = Inspections.aliInspections(project);
//                            val tools = Inspections.aliInspections(project) { it.tool is AliBaseInspection }
                            InspectionManager inspectionManager = InspectionManager.getInstance(project);
                            PsiManager psiManager = PsiManager.getInstance(project);
                            AtomicInteger count = new AtomicInteger(0);
                            Boolean hasViolation=false;
                            for (VirtualFile file:virtualFiles){
                                Computable<Boolean> computable=null;
                                PsiFile psiFile = psiManager.findFile(file);
                                if (psiFile==null){
                                    computable=new Computable<Boolean>() {
                                        @Override
                                        public Boolean compute() {
                                            return false;
                                        }
                                    };
                                }
                                Integer curCount = count.incrementAndGet();
                                progress.setText(file.getCanonicalPath());
                                progress.setFraction(curCount.doubleValue() / Double.valueOf(virtualFiles.size()));
                                for (InspectionToolWrapper<?, ?> it:tools){
                                    progress.checkCanceled();
                                    LocalInspectionTool tool = (LocalInspectionTool)it.getTool() ;
                                    AliBaseInspection aliTool = (AliBaseInspection)tool ;
                                    progress.setText2( aliTool.ruleName());
                                    List<ProblemDescriptor> problems = tool.processFile(psiFile, inspectionManager);
                                    if (problems.size() > 0){
                                        computable=new Computable<Boolean>() {
                                            @Override
                                            public Boolean compute() {
                                                return true;
                                            }
                                        };
                                        break;
                                    }else{
                                        computable=new Computable<Boolean>() {
                                            @Override
                                            public Boolean compute() {
                                                return false;
                                            }
                                        };
                                    }
                                }
                                boolean flag=ApplicationManager.getApplication().runReadAction(computable);
                                if (flag){
                                    hasViolation=true;
                                }
                            }
                            result.set(hasViolation);
                        } catch (ProcessCanceledException e) {
                            result.set(false);
                        } catch (Exception e) {
                            log.error(e);
                            exception.set(e);
                        }
                    }
                });
        if (!exception.isNull()) {
            Exception t = exception.get();
            Rethrow.reThrowRuntime(t);
        }

        return result.get();
    }
}
