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
package com.alibaba.p3c.idea.util;
;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.editor.colors.CodeInsightColors;

/**
 * @author caikang
 * @date 2017/02/04
 */
public class HighlightInfoTypes {
    public static HighlightInfoType BLOCKER = new HighlightInfoType.HighlightInfoTypeImpl(HighlightSeverities.BLOCKER,
            CodeInsightColors.ERRORS_ATTRIBUTES);
    public static HighlightInfoType CRITICAL = new HighlightInfoType.HighlightInfoTypeImpl(HighlightSeverities.CRITICAL,
            CodeInsightColors.WARNINGS_ATTRIBUTES);
    public static HighlightInfoType MAJOR = new HighlightInfoType.HighlightInfoTypeImpl(HighlightSeverities.MAJOR,
            CodeInsightColors.WEAK_WARNING_ATTRIBUTES);
}
