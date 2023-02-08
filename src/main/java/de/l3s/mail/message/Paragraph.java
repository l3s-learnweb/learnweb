package de.l3s.mail.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.l3s.learnweb.i18n.MessagesBundle;

public class Paragraph extends Element {

    private final List<Element> children = new ArrayList<>();

    public Paragraph() {
    }

    public Paragraph(String... texts) {
        if (texts != null) {
            for (String text : texts) {
                children.add(new Text(text));
            }
        }
    }

    public Paragraph(Element... elements) {
        if (elements != null) {
            Collections.addAll(children, elements);
        }
    }

    public Paragraph append(Element element) {
        children.add(element);
        return this;
    }

    public Paragraph append(String element) {
        children.add(new Text(element));
        return this;
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final MessagesBundle msg) {
        sb.append("<p").append(buildAttributes()).append(">");
        for (Element child : children) {
            child.buildHtml(sb, msg);
        }
        sb.append("</p>");
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final MessagesBundle msg) {
        for (Element child : children) {
            child.buildPlainText(sb, msg);
        }
        sb.append("\n\n");
    }
}
