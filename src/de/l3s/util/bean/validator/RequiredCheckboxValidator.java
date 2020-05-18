package de.l3s.util.bean.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.ValidatorException;

@FacesValidator
public class RequiredCheckboxValidator extends AbstractValidator<Object> {
    @Override
    public void validate(FacesContext context, UIComponent component, Object value)
        throws ValidatorException {
        if (value.equals(Boolean.FALSE)) {
            throw new ValidatorException(getFacesMessage(context, component, FacesMessage.SEVERITY_ERROR, "validation.please_confirm"));
        }
    }
}
