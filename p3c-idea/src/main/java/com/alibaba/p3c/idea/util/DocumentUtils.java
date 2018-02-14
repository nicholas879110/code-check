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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;

/**
 *
 *
 * @author caikang
 * @date 2017/03/16
6
 */
public class DocumentUtils {
    private static int PMD_TAB_SIZE = 8;
    public static int  calculateRealOffset(Document document , int line, int pmdColumn) {
        int maxLine = document.getLineCount();
        if (maxLine < line) {
            return -1;
        }
        int lineOffset = document.getLineStartOffset(line - 1);
        return lineOffset + calculateRealColumn(document, line, pmdColumn);
    }

    public static int  calculateRealColumn(Document document, int line,int  pmdColumn) {
        int  realColumn = pmdColumn - 1;
        int minusSize = PMD_TAB_SIZE - 1;
        int docLine = line - 1;
        int lineStartOffset = document.getLineStartOffset(docLine);
        int lineEndOffset = document.getLineEndOffset(docLine);
        String  text = document.getText(new TextRange(lineStartOffset, lineEndOffset));

        int index$iv = 0;
        for(int var13 = 0; var13 < text.length(); ++var13) {
            char item$iv = text.charAt(var13);
            int i = index$iv++;
            if (item$iv == '\t') {
                realColumn -= minusSize;
            }

            if (i >= realColumn) {
                ;
            }
        }

//        text.forEachIndexed { i, c ->
//            if (c == '\t') {
//                realColumn -= minusSize;
//            }
//            if (i >= realColumn) {
//                return@forEachIndexed
//            }
//        }

        return realColumn;
    }
}