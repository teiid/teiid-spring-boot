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

import org.teiid.spring.connections.FileConnectionFactory;

@Target(TYPE)
@Retention(RUNTIME)
public @interface TextTable {
    
    /**
     * DELIMITER sets the field delimiter character to use. Defaults to ','
     */
    String delimiter() default ",";
    
    /**
     * HEADER specifies the text line number (counting every new line) on which
     * the column names occur. If the HEADER option for a column is specified,
     * then that will be used as the expected header name. All lines prior to
     * the header will be skipped. If HEADER is specified, then the header line
     * will be used to determine the TEXTTABLE column position by
     * case-insensitive name matching. This is especially useful in situations
     * where only a subset of the columns are needed. If the HEADER value is not
     * specified, it defaults to 1. If HEADER is not specified, then columns are
     * expected to match positionally with the text contents.
     */
    int header() default 1;
    
    /**
     * SKIP specifies the number of text lines (counting every new line) to skip
     * before parsing the contents. HEADER may still be specified with SKIP.
     */
    int skip() default 0;
    /**
     * QUOTE sets the quote, or qualifier, character used to wrap field values. Defaults to '"'.+
     */
    char quote() default '\"';
    
    /**
     * ESCAPE sets the escape character to use if no quoting character is in
     * use. This is used in situations where the delimiter or new line
     * characters are escaped with a preceding character, e.g. \,
     */
    char escape() default '\\';
    
    /**
     * Source name; If overriding the {@link FileConnectionFactory} bean then provide the name of the bean
     */
    String source() default "file"; // this the default file connection manager
    
    /**
     * On Class ONLY, defines the file to be read. Either relative to source's parent directory or absolute path
     */
    String file();
    
    /**
     * Can be defined table or columns, NO TRIM specified on the TEXTTABLE, it
     * will affect all column and header values. If NO TRIM is specified on a
     * column, then the fixed or unqualified text value not be trimmed of
     * leading and trailing white space.
     */
    boolean notrim() default true;
    
    // column properties
    /**
     * On Column ONLY, this defines the column as ordinal identity column. The data type must be integer.
     * A FOR ORDINALITY column is typed as integer and will return the 1-based item number as its value.
     */
    boolean ordinal() default false;
    
    /**
     * On Column ONLY, WIDTH indicates the fixed-width length of a column in
     * characters - not bytes. With the default ROW DELIMITER, a CR NL sequence
     * counts as a single character.
     */
    int width() default 0;
}
