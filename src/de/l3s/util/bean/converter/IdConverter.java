package de.l3s.util.bean.converter;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.LanguageBundle;

/**
 * This converter tries to convert the input to a positive integer.
 * If it fails it throws an exception but in contrast to the default converter it also adds a global faces
 * message and prevents loading the main content of the page
 */
@FacesConverter("idConverter")
public class IdConverter implements Converter<Integer> {
    //private static final Logger log = LogManager.getLogger(IdConverter.class);

    @Override
    public Integer getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        int valueInt = -1;
        try {
            valueInt = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        if (valueInt < 0) {
            addMessage(context, component, FacesMessage.SEVERITY_FATAL, ("has to be a positive number"));
            throw new ConverterException();
        }

        return valueInt;
    }

    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Integer value) throws ConverterException {
        if (null == value) {
            return null;
        }

        return value.toString();
    }

    protected void addMessage(FacesContext context, UIComponent component, FacesMessage.Severity severity, String msgKey, Object... args) {
        String name = (String) component.getAttributes().get("name");
        String message = LanguageBundle.getLocaleMessage(context.getViewRoot().getLocale(), msgKey, args);

        if (StringUtils.isNotBlank(name)) {
            message = "Parameter '" + name + "': " + message;
        }

        context.addMessage(null, new FacesMessage(severity, message, null));

        if (FacesMessage.SEVERITY_FATAL == severity) {
            HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
            req.setAttribute("hideContent", true);
        }

    }
}
