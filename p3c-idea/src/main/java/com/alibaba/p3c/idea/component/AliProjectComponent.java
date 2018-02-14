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
package com.alibaba.p3c.idea.component;

;
import com.alibaba.p3c.idea.compatible.inspection.Inspections;
import com.alibaba.p3c.idea.config.P3cConfig;
import com.alibaba.p3c.idea.inspection.AliPmdInspectionInvoker;
import com.alibaba.p3c.idea.pmd.SourceCodeProcessor;
import com.alibaba.smartfox.idea.common.component.AliBaseProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

/**
 * @author caikang
 * @date 2016/12/13
 */
public class AliProjectComponent extends AliBaseProjectComponent {

    private Project project;
    P3cConfig p3cConfig;

    public AliProjectComponent(Project project, P3cConfig p3cConfig) {
        this.project = project;
        this.p3cConfig = p3cConfig;
    }

    private VirtualFileListener listener=new VirtualFileAdapter() {

        @Override
        public void contentsChanged(VirtualFileEvent event){
            String path = event.getFile().getCanonicalPath();
            if (path == null || !(path.endsWith(javaExtension) || path.endsWith(velocityExtension))) {
                return;
            }
            PsiFile psiFile=PsiManager.getInstance(project).findFile(event.getFile());
            if (psiFile==null){return;}
            if (!p3cConfig.isRuleCacheEnable()) {
                AliPmdInspectionInvoker.refreshFileViolationsCache(event.getFile());
            }
            if (!p3cConfig.isAstCacheEnable()) {
                SourceCodeProcessor.invalidateCache(path);
            }

        }
    };
    private String javaExtension = ".java";
    private String velocityExtension = ".vm";



    @Override
    public void projectOpened() {
        Inspections.addCustomTag(project, "date");
        VirtualFileManager.getInstance().addVirtualFileListener(listener);
    }

    @Override
    public void projectClosed() {
        VirtualFileManager.getInstance().removeVirtualFileListener(listener);
    }
}
