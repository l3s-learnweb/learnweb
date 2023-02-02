package de.l3s.learnweb.i18n.bundles;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Enables constant substitution in property values.
 * <p>
 * Example:
 * prop1=Lorem
 * prop2=#[prop1] ipsum
 * <p>
 * Inspired by: http://www2.sys-con.com/ITSG/virtualcd/Java/archives/0612/mair/index.html
 *
 * @author Philipp Kemkes
 */
public class RecursiveResourceBundle extends ResourceBundle {
    private static final String START_CONST = "#[";
    private static final String END_CONST = "]";

    private final Map<String, Object> lookup;

    public RecursiveResourceBundle(Map<String, Object> lookup) {
        this.lookup = lookup;

        boolean replacedAtLeastOneConstant;
        do {
            replacedAtLeastOneConstant = false;

            for (Map.Entry<String, Object> entry : lookup.entrySet()) {
                String newValue = substituteValue(entry.getValue().toString());
                if (!entry.getValue().equals(newValue)) {
                    replacedAtLeastOneConstant = true;
                    this.lookup.replace(entry.getKey(), newValue);
                }
            }
        } while (replacedAtLeastOneConstant);
    }

    @Override
    protected Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }

        Object value = lookup.get(key);
        // to make sure we do not show questions marks on frontend like "??? key ???", show key instead
        return value != null ? value : key;
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(lookup.keySet());
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
                // Terminating symbol not found, return the value as is
                return value;
            }

            String constName = sb.substring(startName + START_CONST.length(), endName);
            String constValue = lookup.get(constName).toString();

            if (constValue == null || constValue.contains(START_CONST)) {
                // Property name not found or contains variable, ignore this variable
                beginIndex = endName + END_CONST.length();
                continue;
            }

            // Insert the constant value into the original property value
            sb.replace(startName, endName + END_CONST.length(), constValue);

            // continue checking for constants at this index
            beginIndex += constValue.length();
        }

        return sb.toString();
    }
}
