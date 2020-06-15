package de.l3s.learnweb.component;

import java.io.IOException;
import java.text.DateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.ocpsoft.prettytime.PrettyTime;

@FacesComponent(createTag = true, tagName = "timeAgo", namespace = "http://l3s.de/learnweb")
public class LearnwebTimeAgo extends UIComponentBase {
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component.LearnwebTimeAgo";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        Date date = (Date) getAttributes().get("date");
        ZoneId timeZone = ZoneId.of((String) getAttributes().get("timeZone"));
        String styleClass = (String) getAttributes().get("styleClass");

        UIViewRoot viewRoot = context.getViewRoot();
        Locale locale = viewRoot.getLocale();

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale);
        df.setTimeZone(TimeZone.getTimeZone(timeZone));

        PrettyTime prettyTime = new PrettyTime(locale);

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("span", this);
        writer.writeAttribute("title", df.format(date), null);
        if (styleClass != null) {
            writer.writeAttribute("styleClass", styleClass, "styleClass");
        }
        writer.write(prettyTime.format(date));
        writer.endElement("span");
    }
}
