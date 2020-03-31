package de.l3s.util.bean.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.validator.Validator;

import de.l3s.learnweb.beans.UtilBean;

/**
 * Adds a helper method to the Validator interface
 *
 * @author Philipp
 *
 */
public abstract class AbstractValidator<T> implements Validator<T>
{

    /**
     * Creates a faces message for the given parameters. Tries to translate the message and to add the label of the input element in front of the
     * message.
     *
     * @param component
     * @param severity
     * @param message
     * @return
     */
    public FacesMessage getFacesMessage(UIComponent component, FacesMessage.Severity severity, String message)
    {
        String validatorMessage = ((UIInput) component).getValidatorMessage();

        if(validatorMessage == null)
        {
            Object label = component.getAttributes().get("label");
            if(label == null || (label instanceof String && ((String) label).length() == 0))
            {
                label = component.getValueExpression("label");
            }

            if(label == null)
            {
                validatorMessage = UtilBean.getLocaleMessage(message);
            }
            else
            {
                validatorMessage = label + ": " + UtilBean.getLocaleMessage(message);
            }
        }
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, validatorMessage, validatorMessage);
    }
}
