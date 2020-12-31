package de.l3s.util.bean.validator;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.Validator;

import de.l3s.learnweb.LanguageBundle;

/**
 * Adds a helper method to the Validator interface.
 *
 * @author Philipp Kemkes
 */
public abstract class AbstractValidator<T> implements Validator<T> {

    /**
     * Creates a faces message for the given parameters. Tries to translate the message and to add the label of the input
     * element in front of the message.
     */
    public FacesMessage getFacesMessage(FacesContext context, UIComponent component, FacesMessage.Severity severity, String message) {
        String validatorMessage = ((UIInput) component).getValidatorMessage();

        if (validatorMessage == null) {
            Object label = component.getAttributes().get("label");
            if (label == null || (label instanceof String && ((String) label).isEmpty())) {
                label = component.getValueExpression("label");
            }

            validatorMessage = LanguageBundle.getLocaleMessage(context.getViewRoot().getLocale(), message);
            if (label != null) {
                validatorMessage = label + ": " + validatorMessage;
            }
        }
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, validatorMessage, validatorMessage);
    }
}
