package de.l3s.util.bean.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;

@FacesValidator
public class EmailValidator extends AbstractValidator<Object> {
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value instanceof String) {
            String email = ((String) value).trim().toLowerCase();

            if (StringUtils.endsWithAny(email, "aulecsit.uniud.it", "uni.au.dk", "studeniti.unisalento.it")) {
                String message;
                if (email.endsWith("aulecsit.uniud.it")) {
                    message = "This mail address is invalid! Usually it is surname.name@spes.uniud.it";
                } else {
                    message = "This mail address is invalid! Check the domain.";
                }
                throw new ValidatorException(getFacesMessage(context, component, FacesMessage.SEVERITY_ERROR, message));
            }
        }
    }
}
