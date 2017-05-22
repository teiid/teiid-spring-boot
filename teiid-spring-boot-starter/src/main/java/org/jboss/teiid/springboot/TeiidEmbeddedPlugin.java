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

package org.jboss.teiid.springboot;

import java.util.ResourceBundle;

import org.teiid.core.BundleUtil;

/**
 * @author Kylin Soong
 */
public class TeiidEmbeddedPlugin {
    
    private static final String PLUGIN_ID = "org.jboss.teiid.springboot";
    private static final String BUNDLE_NAME = PLUGIN_ID + ".i18n"; //$NON-NLS-1$
    public static final BundleUtil Util = new BundleUtil(PLUGIN_ID,BUNDLE_NAME,ResourceBundle.getBundle(BUNDLE_NAME));
    
    public static enum Event implements BundleUtil.Event {
        TEIID42001,
        TEIID42002,
        TEIID42003,
        TEIID42004,
        TEIID42005,
        TEIID42006,
        TEIID42007,
        TEIID42008,
        TEIID42009,
        TEIID42010
    }

}
