package de.l3s.learnweb.component;

import java.io.IOException;
import java.util.Locale;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.user.User;

@FacesComponent(createTag = true, tagName = "user", namespace = "http://l3s.de/learnweb")
public class LearnwebUser extends UIComponentBase {
    private static final Logger log = LogManager.getLogger(LearnwebUser.class);
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component.LearnwebUser";
    public User user;

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        try {
            user = (User) getAttributes().get("user");
            if (user == null) {
                return;
            }
        } catch (Exception e) {
            log.error("IOException while passing User", e);
            return;
        }

        if (user.isDeleted()) {
            ResponseWriter writer = context.getResponseWriter();

            UIViewRoot viewRoot = context.getViewRoot();
            Locale locale = viewRoot.getLocale();

            writer.startElement("span", this);
            writer.write(LanguageBundle.getLanguageBundle(locale).getString("deleted_user"));
            writer.endElement("span");
            return;
        }

        super.encodeAll(context);
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        String styleClass = (String) getAttributes().get("styleClass");
        String style = (String) getAttributes().get("style");
        int userId = user.getId();

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("a", this);
        writer.writeAttribute("href", ("user/detail.jsf?user_id=" + userId), null);
        if (styleClass != null) {
            writer.writeAttribute("class", styleClass, null);
        }
        if (style != null) {
            writer.writeAttribute("style", style, null);
        }
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        if (user != null && !user.isDeleted()) {
            if (this.getChildCount() > 0) {
                if (this.getRendersChildren()) {
                    this.encodeChildren(context);
                }
            } else {
                writer.write(user.getUsername());
            }
            writer.endElement("a");
        }
    }
}
