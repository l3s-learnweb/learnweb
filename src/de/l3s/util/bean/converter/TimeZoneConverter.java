package de.l3s.util.bean.converter;

import java.util.TimeZone;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter("timeZoneConverter")
public class TimeZoneConverter implements Converter {

    public TimeZoneConverter() {
    }

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return TimeZone.getTimeZone(value);
    }

    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return TimeZone.getTimeZone(value.toString()).getID();
    }
}
