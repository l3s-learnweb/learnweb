package de.l3s.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * These methods primarily used by maintenance utils and tests to read bundle files one by one.
 */
public final class MessagesHelper {
    static final String MESSAGES_DIR = "de/l3s/learnweb/lang";
    static final String MESSAGES_PREFIX = "messages";
    public static final String MESSAGES_BUNDLE = MESSAGES_DIR + "/" + MESSAGES_PREFIX;
    private static final String BASE_NAME = MESSAGES_BUNDLE.replace('/', '.');

    /**
     * Control that loads only property files and set the empty Locale and hence "messages.properties" as fallback.
     */
    private static final ResourceBundle.Control control = new ResourceBundle.Control() {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return new Locale("");
        }

        @Override
        public List<String> getFormats(String baseName) {
            return ResourceBundle.Control.FORMAT_PROPERTIES;
        }
    };

    public static ResourceBundle getMessagesBundle(Locale locale) {
        return ResourceBundle.getBundle(BASE_NAME, locale, control);
    }

    public static Map<String, String> getMessagesFromBundle(final ResourceBundle bundle) {
        final Map<String, String> messages = new HashMap<>();
        Collections.list(bundle.getKeys()).forEach(key -> messages.put(key, bundle.getString(key)));
        return messages;
    }

    public static Properties getMessagesForLocale(String locale) throws IOException {
        if (locale == null) {
            locale = "";
        }
        if (!locale.isEmpty()) {
            locale = "_" + locale;
        }

        Properties properties = new Properties();
        InputStream is = getResourceAsStream(MESSAGES_BUNDLE + locale + ".properties");
        assert is != null;
        properties.load(is);
        return properties;
    }

    public static List<String> getLocales() throws IOException {
        List<String> locales = new ArrayList<>();

        try (InputStream in = getResourceAsStream(MESSAGES_DIR)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String resource;
                while ((resource = br.readLine()) != null) {
                    resource = resource.replace(MESSAGES_PREFIX + "_", "").replace(".properties", "");
                    locales.add(MESSAGES_PREFIX.equals(resource) ? "en" : resource);
                }
            }
        }

        return locales;
    }

    public static List<String> getLocales(Collection<String> except) throws IOException {
        List<String> locales = getLocales();
        locales.removeAll(except);
        return locales;
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    private static InputStream getResourceAsStream(String resource) {
        final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        return in == null ? MessagesHelper.class.getResourceAsStream(resource) : in;
    }
}
