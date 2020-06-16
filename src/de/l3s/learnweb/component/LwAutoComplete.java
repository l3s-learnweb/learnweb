package de.l3s.learnweb.component;

import java.util.Arrays;
import java.util.List;

import org.primefaces.component.autocomplete.AutoComplete;

public class LwAutoComplete extends AutoComplete {

    @Override
    public Object getValue() {
        Object value = super.getValue();

        if (value != null && value.getClass().isArray()) {
            return Arrays.asList((Object[]) value);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(Object value) {
        boolean useArray = "true".equals(getAttributes().get("returnArray"));

        if (useArray && value instanceof List) {
            value = ((List<String>) value).toArray(new String[0]);
        }

        super.setValue(value);
    }
}
