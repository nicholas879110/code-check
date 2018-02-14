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


package com.alibaba.p3c.idea.i18n;

import com.alibaba.p3c.idea.config.P3cConfig;
import com.alibaba.p3c.pmd.I18nResources;
//import com.alibaba.smartfox.idea.common.util.getService;
import com.intellij.CommonBundle;
import com.intellij.openapi.components.ServiceManager;

import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author caikang
 * @date 2017/06/20
 */
public class P3cBundle {

    public static String getMessage(String key) {
        P3cConfig p3cConfig = ServiceManager.getService(P3cConfig.class);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages.P3cBundle", new Locale(p3cConfig.locale), new I18nResources.XmlControl());
        return resourceBundle.getString(key).trim();
    }

    public static String message(String key, Object... params){
        P3cConfig p3cConfig = ServiceManager.getService(P3cConfig.class);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages.P3cBundle", new Locale(p3cConfig.locale), new I18nResources.XmlControl());
        return CommonBundle.message(resourceBundle, key, params).trim();
    }
}
