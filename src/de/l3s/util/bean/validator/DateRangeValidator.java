package de.l3s.util.bean.validator;

import java.util.Calendar;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@FacesValidator("dateRangeValidator")
public class DateRangeValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value == null) {
            return;
        }

        UIInput startDateComponent = (UIInput) component.getAttributes().get("startDate");

        if (!startDateComponent.isValid()) {
            return;
        }

        Date startDate = (Date) startDateComponent.getValue();

        if (startDate == null) {
            return;
        }

        Date endDate;
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime((Date) value);
        if(c.get(java.util.Calendar.HOUR_OF_DAY) == 0 && c.get(java.util.Calendar.MINUTE) == 0 && c.get(java.util.Calendar.SECOND) == 0)
        {
            c.set(java.util.Calendar.HOUR_OF_DAY, 23);
            c.set(java.util.Calendar.MINUTE, 59);
            c.set(java.util.Calendar.SECOND, 59);
            c.set(Calendar.MILLISECOND, 999);
        }
        endDate = c.getTime();

        if (startDate.after(endDate)) {
            startDateComponent.setValid(false);
            FacesContext.getCurrentInstance().addMessage("msg", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Start date may not be after end date.", null));
        }
    }
}