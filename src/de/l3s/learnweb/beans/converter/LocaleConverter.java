package de.l3s.learnweb.beans.converter;

import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.inject.Named;

@Named
public class LocaleConverter implements Converter<Locale>
{

    @Override
    public Locale getAsObject(FacesContext arg0, UIComponent arg1, String value) throws ConverterException
    {
        return Locale.forLanguageTag(value);
    }

    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Locale value) throws ConverterException
    {
        return value.toLanguageTag();
    }

}
