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
package com.alibaba.p3c.idea.config;
;
import com.google.common.collect.Sets;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

/**
 *
 *
 * @author caikang
 * @date 2017/03/01
 */
@State(name = "SmartFoxProjectConfig",storages ={@Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR+"/smartfox_info.xml")})
public class SmartFoxProjectConfig implements PersistentStateComponent<SmartFoxProjectConfig> {
    HashSet<String> inspectionProfileModifiedSet = Sets.newHashSet();

    boolean projectInspectionClosed = false;

    @Nullable
    @Override
    public SmartFoxProjectConfig getState() {
        return this;
    }

    @Override
    public void loadState(SmartFoxProjectConfig state) {
        if (state == null) {
            return;
        }
        XmlSerializerUtil.copyBean(state, this);
    }

}
