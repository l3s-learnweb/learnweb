package de.l3s.learnweb.component;

import java.io.IOException;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

@FacesComponent(createTag = true, tagName = "encryptEmail", namespace = "http://l3s.de/learnweb")
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
        String name = email.split("@")[0];
        String domain = email.split("@")[1].split("\\.")[0];
        String tld = email.split("@")[1].split("\\.")[1];
        writer.startElement("a", this);
        writer.writeAttribute("href", "#", null);
        writer.writeAttribute("data-name", name, null);
        writer.writeAttribute("data-domain", domain, null);
        writer.writeAttribute("data-tld", tld, null);
        writer.writeAttribute("class", "encrypt-email" + (styleClass != null ? " " + styleClass : ""), null);
        writer.writeAttribute("onclick", "window.location.href = 'mailto:' + this.dataset.name + '@' + this.dataset.domain + '.' + this.dataset.tld; return false;", null);
        writer.endElement("a");
    }
}
