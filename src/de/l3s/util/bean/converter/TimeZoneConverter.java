package de.l3s.util.bean.converter;

import java.time.ZoneId;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter("timeZoneConverter")
public class TimeZoneConverter implements Converter<ZoneId> {

    @Override
    public ZoneId getAsObject(FacesContext context, UIComponent component, String value) {
        return ZoneId.of(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ZoneId value) {
        return value.getId();
    }
}
