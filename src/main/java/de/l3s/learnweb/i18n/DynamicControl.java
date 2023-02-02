package de.l3s.learnweb.i18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.i18n.bundles.DebugResourceBundle;
import de.l3s.learnweb.i18n.bundles.RecursiveResourceBundle;

/**
 * Control that loads property files and set the empty Locale and hence "messages.properties" as fallback.
 */
public class DynamicControl extends ResourceBundle.Control {
    private static final Logger log = LogManager.getLogger(DynamicControl.class);
    public static final String BASE_NAME = "de.l3s.learnweb.lang.messages";

    @Override
    public Locale getFallbackLocale(String baseName, Locale locale) {
        return new Locale("");
    }

    @Override
    public List<String> getFormats(String baseName) {
        return ResourceBundle.Control.FORMAT_PROPERTIES;
    }

    @Override
    public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload)
        throws IllegalAccessException, InstantiationException, IOException {
        log.debug("Bundle requested: {} {} {} {}", baseName, locale, format, reload);

        if ("xx".equals(locale.getLanguage())) {
            return new DebugResourceBundle();
        }

        ResourceBundle source = ResourceBundle.getBundle(BASE_NAME, locale);
        log.debug("Bundle loaded: {} {}", source.getLocale(), source.getBaseBundleName());
        ArrayList<String> keys = Collections.list(source.getKeys());
        HashMap<String, Object> lookup = new HashMap<>(keys.size());
        keys.forEach(key -> lookup.put(key, source.getObject(key)));
        return new RecursiveResourceBundle(lookup);
    }
}
