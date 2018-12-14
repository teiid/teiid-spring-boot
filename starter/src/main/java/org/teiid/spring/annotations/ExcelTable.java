/*
 * Copyright 2012-2014 the original author or authors.
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
package org.teiid.spring.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation on any Entity class, that defines the data from a Excel
 * file. Note that only 2 dimensional tabular structure based content is
 * supported. Must define "file" attribute to define where to read from. This
 * annotation is to read a Excel file into an Entity from a file location.<br>
 * See Excel translator in Teiid for more information <a href=
 * "http://teiid.github.io/teiid-documents/master/content/reference/Microsoft_Excel_Translator.html">[Microsoft
 * Excel Translator]</a>
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface ExcelTable {

    /**
     * On Class ONLY, defines the excel file to be read. Either relative to source's
     * parent directory or absolute path
     *
     * @return excel file name
     */
    String file();

    /**
     * Row number from which data rows start from
     *
     * @return row number
     */
    int dataRowStartsAt() default 0;

    /**
     * When true any cells with empty value for header row are ignored, otherwise an
     * empty header row cell indicates end of columns.
     *
     * @return true to ignore
     */
    boolean ignoreEmptyCells() default false;

    /**
     * Row number that contains the header information, -1 no header
     *
     * @return header column row number
     */
    int headerRow() default -1;

    /**
     * Name of the Sheet to read the data from
     *
     * @return sheet name
     */
    String sheetName() default "Sheet1";
}
