package de.l3s.util.bean.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.ValidatorException;

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
