package de.l3s.util.bean.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import de.l3s.learnweb.beans.exceptions.BadRequestBeanException;
import de.l3s.util.StringHelper;

/**
 * This converter tries to convert the input to a positive integer.
 * If it fails it throws BadRequestBeanException and prevents loading the main content of the page.
 *
 * TODO: instead of throwing error, possible, we can use o:viewParamValidationFailed
 * http://showcase.omnifaces.org/taghandlers/viewParamValidationFailed
 */
@FacesConverter("idConverter")
public class IdConverter implements Converter<Integer> {

    @Override
    public Integer getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        int valueInt = StringHelper.parseInt(value);

        if (valueInt < 0) {
            throw new BadRequestBeanException();
        }

        return valueInt;
    }

    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Integer value) throws ConverterException {
        if (null == value) {
            return null;
        }

        return value.toString();
    }
}
