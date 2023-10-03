package de.l3s.learnweb.component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

import org.ocpsoft.prettytime.PrettyTime;

@FacesComponent(createTag = true, tagName = "timeAgo", namespace = "http://l3s.de/learnweb")
public class LearnwebTimeAgo extends UIComponentBase {
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component.LearnwebTimeAgo";

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        TemporalAccessor dateTime = (TemporalAccessor) getAttributes().get("date");
        ZoneId timeZone = ZoneId.of((String) getAttributes().get("timeZone"));
        String styleClass = (String) getAttributes().get("styleClass");

        UIViewRoot viewRoot = context.getViewRoot();
        Locale locale = viewRoot.getLocale();

        PrettyTime prettyTime = new PrettyTime(locale);

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("span", this);
        writer.writeAttribute("title", formatter.localizedBy(locale).withZone(timeZone).format(dateTime), null);
        if (styleClass != null) {
            writer.writeAttribute("class", styleClass, "styleClass");
        }
        if (dateTime instanceof Instant instant) {
            writer.write(prettyTime.format(instant));
        } else {
            writer.write(prettyTime.format(LocalDateTime.from(dateTime).atZone(timeZone)));
        }
        writer.endElement("span");
    }
}
