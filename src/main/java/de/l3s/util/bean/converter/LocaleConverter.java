package de.l3s.util.bean.converter;

import java.util.Locale;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@FacesConverter("localeConverter")
public class LocaleConverter implements Converter<Locale> {
    private static final Logger log = LogManager.getLogger(LocaleConverter.class);

    @Override
    public Locale getAsObject(FacesContext arg0, UIComponent arg1, String value) throws ConverterException {
        if (null == value) {
            log.error("Locale value is null");
            return Locale.ENGLISH;
        }
        return Locale.forLanguageTag(value);
    }

    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Locale value) throws ConverterException {
        if (null == value) {
            log.error("Locale is null");
            return null;
        }

        return value.toLanguageTag();
    }
}
