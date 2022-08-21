package de.l3s.util.bean.validator;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.app.Learnweb;

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

            Learnweb.dao().getBounceDao().findByEmail(email).ifPresent(bounce -> {
                String message = "In the past emails to " + email + " could not be delivered. On " + bounce.received()
                    + " we received the following error: " + bounce.description();
                throw new ValidatorException(getFacesMessage(context, component, FacesMessage.SEVERITY_ERROR, message));
            });
        }
    }
}
