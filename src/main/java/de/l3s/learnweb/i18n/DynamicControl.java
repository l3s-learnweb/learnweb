package de.l3s.learnweb.i18n;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.i18n.bundles.DebugResourceBundle;
import de.l3s.learnweb.i18n.bundles.SizedResourceBundle;

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
    public List<String> getFormats(final String baseName) {
        return FORMAT_PROPERTIES;
    }

    @Override
    public long getTimeToLive(final String baseName, final Locale locale) {
        return TTL_DONT_CACHE;
    }

    @Override
    public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload)
        throws IllegalAccessException, InstantiationException, IOException {
        log.debug("Bundle requested: '{}' {}", locale, reload);

        if ("xx".equals(locale.getLanguage())) {
            return new DebugResourceBundle();
        }

        ResourceBundle bundle = super.newBundle(BASE_NAME, locale, format, loader, reload);
        if (bundle == null) {
            log.debug("Bundle not found: '{}'", locale);
            return null;
        }

        log.debug("Bundle loaded: '{}', keys {}", locale, bundle.keySet().size());
        ResourceBundle result = new SizedResourceBundle(bundle);
        return result;
    }
}
