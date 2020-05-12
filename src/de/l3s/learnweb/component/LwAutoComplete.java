package de.l3s.learnweb.component;

import java.util.Arrays;
import java.util.List;

import org.primefaces.component.autocomplete.AutoComplete;

public class LwAutoComplete extends AutoComplete
{
    private boolean isInputValueArray = false;

    @Override
    public Object getValue()
    {
        Object value = super.getValue();

        if(value != null && value.getClass().isArray())
        {
            isInputValueArray = true;
            Object[] array = (Object[]) value;
            return Arrays.asList(array);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(Object value)
    {
        if(isInputValueArray && value instanceof List)
        {
            value = ((List<String>) value).toArray(new String[0]);
        }

        super.setValue(value);
    }
}
