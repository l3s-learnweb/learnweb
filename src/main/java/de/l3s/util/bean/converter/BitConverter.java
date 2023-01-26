package de.l3s.util.bean.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

import org.apache.commons.lang3.StringUtils;

@FacesConverter("BitConverter")
public class BitConverter implements Converter<Boolean> {

    @Override
    public Boolean getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        if (StringUtils.isEmpty(submittedValue)) {
            return null;
        }

        return "1".equals(submittedValue) || "true".equals(submittedValue);
    }

    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Boolean value) throws ConverterException {
        if (null == value) {
            return null;
        }

        return value ? "1" : "0";
    }
}
