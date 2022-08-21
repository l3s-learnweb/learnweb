package de.l3s.util.bean.validator;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.ValidatorException;

import de.l3s.util.UrlHelper;

@FacesValidator
public class UrlValidator extends AbstractValidator<Object> {
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value instanceof String) {
            String url = ((String) value).trim();

            if (UrlHelper.validateUrl(url) == null) {
                throw new ValidatorException(getFacesMessage(context, component, FacesMessage.SEVERITY_ERROR, "invalid_url"));
            }
        }
    }
}
