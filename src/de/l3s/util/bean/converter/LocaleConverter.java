package de.l3s.util.bean.converter;

import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.util.bean.BeanHelper;

@Named
public class LocaleConverter implements Converter<Locale>
{
    private static final Logger log = Logger.getLogger(LocaleConverter.class);

    @Override
    public Locale getAsObject(FacesContext arg0, UIComponent arg1, String value) throws ConverterException
    {
        if(null == value)
        {
            log.warn("value is null: " + BeanHelper.getRequestSummary());
            return Locale.ENGLISH;
        }
        return Locale.forLanguageTag(value);
    }

    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Locale value) throws ConverterException
    {
        if(null == value)
        {
            log.warn("Locale is null: " + BeanHelper.getRequestSummary());
            return null;
        }

        return value.toLanguageTag();
    }
}