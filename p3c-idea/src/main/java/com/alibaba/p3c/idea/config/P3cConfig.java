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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;


/**
 * @author caikang
 * @date 2017/06/19
 */
@State(name = "P3cConfig", storages = {@Storage(file = StoragePathMacros.APP_CONFIG+"/smartfox/p3c.xml")})
public class P3cConfig implements PersistentStateComponent<P3cConfig> {
    long astCacheTime = 1000L;
    boolean astCacheEnable = true;

    long ruleCacheTime = 1000L;
    boolean ruleCacheEnable = false;

    public static boolean analysisBeforeCheckin = false;

    public String locale = localeZh;


    @Nullable
    @Override
    public P3cConfig getState() {
        return this;
    }

    @Override
    public void loadState(P3cConfig state) {
        if (state == null) {
            return;
        }
        XmlSerializerUtil.copyBean(state, this);
    }


    public static String localeEn = "en";
    public static String localeZh = "zh";

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public long getAstCacheTime() {
        return astCacheTime;
    }

    public void setAstCacheTime(long astCacheTime) {
        this.astCacheTime = astCacheTime;
    }

    public boolean isAstCacheEnable() {
        return astCacheEnable;
    }

    public void setAstCacheEnable(boolean astCacheEnable) {
        this.astCacheEnable = astCacheEnable;
    }

    public long getRuleCacheTime() {
        return ruleCacheTime;
    }

    public void setRuleCacheTime(long ruleCacheTime) {
        this.ruleCacheTime = ruleCacheTime;
    }

    public boolean isRuleCacheEnable() {
        return ruleCacheEnable;
    }

    public void setRuleCacheEnable(boolean ruleCacheEnable) {
        this.ruleCacheEnable = ruleCacheEnable;
    }

}