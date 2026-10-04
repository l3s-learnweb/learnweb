package de.l3s.util.bean.validator;

import java.util.Date;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.ValidatorException;

import org.apache.commons.lang3.Strings;

import de.l3s.learnweb.app.Learnweb;

@FacesValidator
public class EmailValidator extends AbstractValidator<Object> {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value instanceof String strValue) {
            String email = strValue.trim().toLowerCase();

            if (Strings.CS.endsWithAny(email, "aulecsit.uniud.it", "uni.au.dk", "studeniti.unisalento.it")) {
                String message = email.endsWith("aulecsit.uniud.it") ? "email_invalid_uniud" : "email_invalid_domain";
                throw new ValidatorException(getFacesMessage(context, component, FacesMessage.SEVERITY_ERROR, message));
            }

            Learnweb.dao().getBounceDao().findByEmail(email).ifPresent(bounce -> {
                throw new ValidatorException(getFacesMessage(context, component, FacesMessage.SEVERITY_ERROR, "email_bounced",
                    email, Date.from(bounce.received()), bounce.description()));
            });
        }
    }
}
