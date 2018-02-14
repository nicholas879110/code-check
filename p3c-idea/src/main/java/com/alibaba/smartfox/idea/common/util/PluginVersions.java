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
package com.alibaba.smartfox.idea.common.util;

;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;

/**
 * @author caikang
 */
public class PluginVersions {
   public static final int baseVersion141 = 141;
   public static final int baseVersion143 = 143;
   public static final int baseVersion145 = 145;
   public static final int baseVersion162 = 162;
   public static final int baseVersion163 = 163;
   public static final int baseVersion171 = 171;

    public static PluginId pluginId = ((PluginClassLoader) (PluginVersions.class.getClassLoader())).getPluginId();
    IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(pluginId);

    /**
     * 获取当前安装的 plugin版本
     */
    String getPluginVersion() {
        return pluginDescriptor.getVersion();
    }

    /**
     * 获取当前使用的IDE版本
     */
    public String getIdeVersion() {
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        return applicationInfo.getFullVersion() + "_" + applicationInfo.getBuild();
    }


    public  static int getBaseVersion() {
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        return applicationInfo.getBuild().getBaselineVersion();
    }
}
