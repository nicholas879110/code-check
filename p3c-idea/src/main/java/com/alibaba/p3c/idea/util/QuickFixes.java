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

import com.alibaba.p3c.idea.quickfix.*;
import com.intellij.codeInspection.LocalQuickFix;

import java.util.HashMap;
import java.util.Map;

/**
 * @author caikang
 * @date 2017/02/06
 */
public class QuickFixes {
    private static Map<String, AliQuickFix> quickFixes =
            new HashMap() {{
                put(VmQuietReferenceQuickFix.ruleName, new VmQuietReferenceQuickFix());
                put(ClassMustHaveAuthorQuickFix.ruleName, new ClassMustHaveAuthorQuickFix());
                put(ConstantFieldShouldBeUpperCaseQuickFix.ruleName, new ConstantFieldShouldBeUpperCaseQuickFix());
                put(AvoidStartWithDollarAndUnderLineNamingQuickFix.ruleName, new AvoidStartWithDollarAndUnderLineNamingQuickFix());
                put(LowerCamelCaseVariableNamingQuickFix.ruleName, new LowerCamelCaseVariableNamingQuickFix());
            }};
//    mutableMapOf(VmQuietReferenceQuickFix.ruleName to VmQuietReferenceQuickFix,
//                 ClassMustHaveAuthorQuickFix.ruleName to ClassMustHaveAuthorQuickFix,
//                 ConstantFieldShouldBeUpperCaseQuickFix.ruleName to ConstantFieldShouldBeUpperCaseQuickFix,
//                 AvoidStartWithDollarAndUnderLineNamingQuickFix.ruleName to AvoidStartWithDollarAndUnderLineNamingQuickFix,
//                 LowerCamelCaseVariableNamingQuickFix.ruleName to LowerCamelCaseVariableNamingQuickFix);


    public static LocalQuickFix getQuickFix(String rule, Boolean isOnTheFly) {
        AliQuickFix quickFix = quickFixes.get(rule);
        if (quickFix == null) return null;
        if (!quickFix.getOnlyOnThFly()) {
            return quickFix;
        }
        if (!isOnTheFly && quickFix.getOnlyOnThFly()) {
            return null;
        }
        return quickFix;
    }
}
