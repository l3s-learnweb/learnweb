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
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * This methods primarily used by maintenance utils and tests to read bundle files one by one.
 *
 * @author Oleh Astappiev
 */
public final class MessagesHelper {
    static final String MESSAGES_DIR = "de/l3s/learnweb/lang";
    static final String MESSAGES_PREFIX = "messages";
    public static final String MESSAGES_BUNDLE = MESSAGES_DIR + "/" + MESSAGES_PREFIX;

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
