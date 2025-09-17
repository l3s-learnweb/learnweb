package de.l3s.learnweb.component;

import java.io.IOException;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

@FacesComponent(createTag = true, tagName = "encryptEmail", namespace = "learnweb")
public class LearnwebEncryptEmail extends UIComponentBase {
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component.LearnwebEncryptEmail";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        String styleClass = (String) getAttributes().get("styleClass");
        String email = (String) getAttributes().get("email");

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("a", this);
        writer.writeAttribute("href", "#", null);

        String[] emailParts = email.split("@");
        if (emailParts.length == 2) {
            String name = emailParts[0];
            int lastDotIndex = emailParts[1].lastIndexOf('.');

            writer.writeAttribute("data-name", name, null);
            writer.writeAttribute("data-domain", emailParts[1].substring(0, lastDotIndex), null);
            writer.writeAttribute("data-tld", emailParts[1].substring(lastDotIndex + 1), null);
            writer.writeAttribute("class", "encrypt-email" + (styleClass != null ? " " + styleClass : ""), null);
            writer.writeAttribute("onclick", "window.location.href = 'mailto:' + this.dataset.name + '@' + this.dataset.domain + '.' + this.dataset.tld; return false;", null);
        } else {
            writer.writeAttribute("onclick", "return false;", null);
            // it can be hashed email, which should not be clickable
            writer.write(email);
        }
        writer.endElement("a");
    }
}
