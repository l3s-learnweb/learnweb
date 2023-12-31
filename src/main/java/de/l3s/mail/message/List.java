package de.l3s.mail.message;

import java.util.ArrayList;

import de.l3s.learnweb.i18n.MessagesBundle;

public class List extends Element {

    private final java.util.List<Element> children = new ArrayList<>();

    public List add(Element element) {
        children.add(element);
        return this;
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final MessagesBundle msg) {
        sb.append("<ul").append(buildAttributes()).append(">");
        for (Element child : children) {
            sb.append("<li>");
            child.buildHtml(sb, msg);
            sb.append("</li>");
        }
        sb.append("</ul>");
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final MessagesBundle msg) {
        sb.append("\n");
        for (Element child : children) {
            sb.append("* ");
            child.buildPlainText(sb, msg);
            sb.append("\n");
        }
        sb.append("\n");
    }
}
