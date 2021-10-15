package de.l3s.learnweb.component;

import java.io.IOException;
import java.util.Locale;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
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
    public void encodeBegin(FacesContext context) throws IOException {
        String styleClass = (String) getAttributes().get("styleClass");
        String style = (String) getAttributes().get("style");
        try {
            user = (User) getAttributes().get("user");
            if (user == null) {
                return;
            }
        } catch (Exception e) {
            log.error("IOException while passing User", e);
            return;
        }
        int userId = user.getId();

        UIViewRoot viewRoot = context.getViewRoot();
        Locale locale = viewRoot.getLocale();
        ResponseWriter writer = context.getResponseWriter();

        if (user.isDeleted()) {
            writer.write(LanguageBundle.getLanguageBundle(locale).getString("deleted_user"));
            return;
        }
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
                renderChildren(context, this);
            } else {
                writer.write(user.getUsername());
            }
            writer.endElement("a");
        }
    }

    protected void renderChildren(FacesContext context, UIComponent component) throws IOException {
        for (int i = 0; i < component.getChildCount(); i++) {
            UIComponent child = component.getChildren().get(i);
            renderChild(context, child);
        }
    }

    protected void renderChild(FacesContext context, UIComponent child) throws IOException {
        if (!child.isRendered()) {
            return;
        }

        child.encodeBegin(context);

        if (child.getRendersChildren()) {
            child.encodeChildren(context);
        } else {
            renderChildren(context, child);
        }
        child.encodeEnd(context);
    }
}
