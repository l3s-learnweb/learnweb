package de.l3s.learnweb.i18n.bundles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class SizedResourceBundle extends ResourceBundle {
    private final Map<String, Object> lookup;

    public SizedResourceBundle(ResourceBundle source) {
        ArrayList<String> keys = Collections.list(source.getKeys());
        lookup = new HashMap<>(keys.size());
        keys.forEach(key -> lookup.put(key, source.getObject(key)));
    }

    @Override
    protected Object handleGetObject(String key) {
        return lookup.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        ResourceBundle parent = this.parent;
        return new ResourceBundleEnumeration(lookup.keySet(), (parent != null) ? parent.getKeys() : null);
    }

    public int size() {
        return lookup.size();
    }

    public ResourceBundle getParent() {
        return parent;
    }
}
