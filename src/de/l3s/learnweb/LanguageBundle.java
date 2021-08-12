package de.l3s.learnweb;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

/**
 * Enables constant substitution in property values.
 *
 * Example:
 * prop1=Lorem
 * prop2=#[prop1] ipsum
 *
 * Inspired by: http://www2.sys-con.com/ITSG/virtualcd/Java/archives/0612/mair/index.html
 *
 * @author Philipp Kemkes
 */
public class LanguageBundle extends ResourceBundle {
    private static final Logger log = LogManager.getLogger(LanguageBundle.class);

    private static final String BASE_NAME = "de.l3s.learnweb.lang.messages";
    private static final String START_CONST = "#[";
    private static final String END_CONST = "]";

    private static final List<Locale> supportedLocales = Collections.synchronizedList(new ArrayList<>());
    private static final ConcurrentHashMap<Locale, ResourceBundle> cache = new ConcurrentHashMap<>(7);

    /**
     * Control that loads only property files and set the empty Locale and hence "messages.properties" as fallback.
     */
    protected static final Control control = new ResourceBundle.Control() {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return new Locale("");
        }

        @Override
        public List<String> getFormats(String baseName) {
            return ResourceBundle.Control.FORMAT_PROPERTIES;
        }
    };

    static {
        cache.put(new Locale("xx"), new DebugBundle());
    }

    private Map<String, String> values;

    public LanguageBundle() { // this is required by JSF because we can't pass arguments to the default constructor
        this(Faces.getViewRoot().getLocale());
    }

    public LanguageBundle(Locale locale) {
        setParent(getLanguageBundle(locale));
    }

    private LanguageBundle(ResourceBundle sourceBundle) {
        // iterate over keys an replace constants of type #[key_name]
        ArrayList<String> keys = Collections.list(sourceBundle.getKeys());

        values = new HashMap<>(keys.size());
        for (String key : keys) {
            String value = sourceBundle.getString(key);

            values.put(key, value);
        }

        boolean replacedAtLeastOneConstant;
        do {
            replacedAtLeastOneConstant = false;

            for (Entry<String, String> entry : values.entrySet()) {
                String newValue = substituteValue(entry.getValue());
                if (!entry.getValue().equals(newValue)) {
                    replacedAtLeastOneConstant = true;
                    entry.setValue(newValue);
                }
            }
        } while (replacedAtLeastOneConstant);

        setParent(null); // the entries have been copied -> release memory
    }

    private String substituteValue(String value) {
        if (value == null) {
            return null;
        }

        int beginIndex = 0;
        int startName; // index of the  constant, if any

        StringBuilder sb = new StringBuilder(value); // build the new value

        while (true) {
            startName = sb.indexOf(START_CONST, beginIndex);
            if (startName == -1) {
                break;
            }

            int endName = sb.indexOf(END_CONST, startName);
            if (endName == -1) {
                // Terminating symbol not found
                // Return the value as is

                return value;
            }

            String constName = sb.substring(startName + START_CONST.length(), endName);
            String constValue = values.get(constName);

            if (constValue == null || constValue.contains(START_CONST)) {
                // Property name not found or contains variable
                // Ignore this variable
                beginIndex = endName + END_CONST.length();
                continue;
            }

            // Insert the constant value into the
            // original property value
            sb.replace(startName, endName + END_CONST.length(), constValue);

            // continue checking for constants at this index
            beginIndex += constValue.length();
        }

        return sb.toString();
    }

    @Override
    protected Object handleGetObject(String key) {
        Object value = values != null ? values.get(key) : parent.getObject(key);
        // to make sure we do not show questions marks on frontend like "??? key ???", show key instead
        return value != null ? value : key;
    }

    @Override
    public Enumeration<String> getKeys() {
        return values != null ? Collections.enumeration(values.keySet()) : parent.getKeys();
    }

    public static ResourceBundle getLanguageBundle(Locale locale) {
        return cache.computeIfAbsent(locale, loc -> new LanguageBundle(ResourceBundle.getBundle(BASE_NAME, loc, control)));
    }

    /**
     * @return Supported frontend locales as defined in faces-config.xml
     */
    public static List<Locale> getSupportedLocales() {
        if (supportedLocales.isEmpty()) {
            supportedLocales.addAll(Faces.getSupportedLocales());
        }
        return Collections.unmodifiableList(supportedLocales);
    }

    public static String getLocaleMessage(Locale locale, String msgKey, Object... args) {
        ResourceBundle bundle = getLanguageBundle(locale);

        String msg = "";
        try {
            msg = bundle.getString(msgKey);
            if (args != null) {
                MessageFormat format = new MessageFormat(msg);
                msg = format.format(args);
            }
        } catch (MissingResourceException e) {
            //      log.warn("Missing translation for key: " + msgKey);
            msg = msgKey;
        } catch (IllegalArgumentException e) {
            log.error("Can't translate msgKey={} with msg={}; May happen if the msg contains unexpected curly brackets.", msgKey, msg, e);

            msg = msgKey;
        }
        return msg;
    }

    /**
     * True if the given value is equal to any translation of the given msg key.
     */
    public static boolean isEqualForAnyLocale(String value, String msgKey) {
        for (Locale localeToCheck : getSupportedLocales()) {
            String translation = getLocaleMessage(localeToCheck, msgKey);

            if (!translation.equals(translation.trim())) {
                log.warn(msgKey + " contains whitespaces");
            }

            if (value.equalsIgnoreCase(translation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This bundle returns always the key as it's value. This helps to find the appropriate key in the frontend
     */
    private static class DebugBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String key) {
            return "#" + key + "#";
        }

        @Override
        public Enumeration<String> getKeys() {
            return null;
        }
    }

}
