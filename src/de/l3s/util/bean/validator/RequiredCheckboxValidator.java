package de.l3s.util.bean.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import de.l3s.learnweb.beans.UtilBean;

@FacesValidator
public class RequiredCheckboxValidator implements Validator<Object>
{
    @Override
    public void validate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException
    {
        if(value.equals(Boolean.FALSE))
        {
            String requiredMessage = ((UIInput) component).getRequiredMessage();

            if(requiredMessage == null)
            {
                Object label = component.getAttributes().get("label");
                if(label == null || (label instanceof String && ((String) label).length() == 0))
                {
                    label = component.getValueExpression("label");
                }
                if(label == null)
                {
                    label = component.getClientId(context);
                }

                requiredMessage = label + " " + UtilBean.getLocaleMessage("validation.please_confirm");
            }
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, requiredMessage, requiredMessage));
        }
    }
}
