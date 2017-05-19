package org.jboss.teiid.springboot;

import java.util.ResourceBundle;

import org.teiid.core.BundleUtil;

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
