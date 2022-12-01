package de.l3s.util.bean.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

import de.l3s.learnweb.exceptions.BadRequestHttpException;

/**
 * This converter tries to convert the input to a positive integer.
 * If it fails it throws BadRequestBeanException and prevents loading the main content of the page.
 *
 * TODO @astappiev: instead of throwing error, possible, we can use o:viewParamValidationFailed
 * http://showcase.omnifaces.org/taghandlers/viewParamValidationFailed
 */
@FacesConverter("idConverter")
public class IdConverter implements Converter<Integer> {

    @Override
    public Integer getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        try {
            int valueInt = Integer.parseInt(value);

            if (valueInt < 0) {
                throw new IllegalArgumentException("Negative integer is not acceptable.");
            }

            return valueInt;
        } catch (IllegalArgumentException ignored) {
            throw new BadRequestHttpException("The given parameter is not valid identifier.", true);
        }
    }

    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Integer value) throws ConverterException {
        if (null == value) {
            return null;
        }

        return value.toString();
    }
}
