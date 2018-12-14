/*
 * Copyright 2012-2017 the original author or authors.
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

package org.teiid.spring.autoconfigure;

public interface TeiidConstants {

    String ENTITY_SCAN_DIR = "spring.teiid.model.package";
    String REDIRECTED = "spring.teiid.redirected";

    String REDIRECTED_TABLE_POSTFIX = "_REDIRECTED";

    String VDBNAME = "spring";
    String VDBVERSION = "1.0.0";
    String EXPOSED_VIEW = "teiid";
}
