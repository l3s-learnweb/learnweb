package de.l3s.learnweb;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

import de.l3s.util.MessagesHelper;

public class LanguageBundle extends ResourceBundle {
    private static final Logger log = LogManager.getLogger(LanguageBundle.class);

    private static final ConcurrentHashMap<Locale, ResourceBundle> cache = new ConcurrentHashMap<>(7);

    static {
        cache.put(new Locale("xx"), new DebugBundle());
    }

    private final Locale locale;

    public LanguageBundle() { // this is required by JSF because we can't pass arguments to the default constructor
        this(Faces.getLocale());
    }

    public LanguageBundle(Locale locale) {
        this.locale = locale;

        setParent(cache.computeIfAbsent(locale, loc -> new DynamicBundle(MessagesHelper.getMessagesBundle(loc))));
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    protected Object handleGetObject(String key) {
        return parent.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return parent.getKeys();
    }

    public String getFormatted(String key, Object... args) {
        String msg = null;
        try {
            msg = parent.getString(key);
            if (args != null) {
                return new MessageFormat(msg).format(args);
            }
        } catch (IllegalArgumentException e) {
            log.error("Can't translate msgKey={} with msg={}; May happen if the msg contains unexpected curly brackets.", key, msg, e);
            return key;
        }
        return msg;
    }

    public static LanguageBundle getBundle(Locale locale) {
        return new LanguageBundle(locale);
    }

    /**
     * This bundle returns always the key as its value. This helps to find the appropriate key on the frontend
     */
    private static class DebugBundle extends ResourceBundle {

        @Override
        protected Object handleGetObject(String key) {
            return "#" + key + "#";
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }
    }

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
    private static class DynamicBundle extends ResourceBundle {
        private static final String START_CONST = "#[";
        private static final String END_CONST = "]";

        private final Map<String, String> values;

        /**
         * Creates a new {@code ResourceBundle} by copying all entries from {@code sourceBundle}.
         * In addition, its iterates over all values and replace constants of type #[key_name]
         */
        protected DynamicBundle(ResourceBundle sourceBundle) {
            // copy content from sourceBundle
            ArrayList<String> keys = Collections.list(sourceBundle.getKeys());
            values = new HashMap<>(keys.size());
            for (String key : keys) {
                String value = sourceBundle.getString(key);
                values.put(key, value);
            }

            // replace constants
            boolean replacedAtLeastOneConstant;
            do {
                replacedAtLeastOneConstant = false;

                for (Map.Entry<String, String> entry : values.entrySet()) {
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
            int startName; // index of the constant, if any

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
            Object value = values.get(key);
            // to make sure we do not show questions marks on frontend like "??? key ???", show key instead
            return value != null ? value : key;
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(values.keySet());
        }
    }
}
