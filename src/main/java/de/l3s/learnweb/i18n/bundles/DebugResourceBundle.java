package de.l3s.learnweb.i18n.bundles;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;

public class DebugResourceBundle extends ResourceBundle {
    @Override
    protected Object handleGetObject(String key) {
        return "[" + key + "]";
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.emptyEnumeration();
    }
}
