package de.l3s.mail.message;

import java.util.Map;

import org.apache.commons.lang3.Validate;

import de.l3s.learnweb.i18n.MessagesBundle;

public class Link extends Element {
    private final String text;
    private final String url;

    public Link(String url) {
        Validate.notBlank(url);
        this.text = null;
        this.url = url;
    }

    public Link(String url, String text) {
        Validate.notBlank(url);
        Validate.notBlank(text);
        this.text = text;
        this.url = url;
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final MessagesBundle msg) {
        sb.append("<a").append(buildAttributes(Map.of("href", url))).append(">");
        if (null == text) {
            sb.append(this.url);
        } else {
            sb.append(msg.format(text));
        }
        sb.append("</a>");
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final MessagesBundle msg) {
        if (text == null) {
            sb.append(url);
        } else {
            sb.append(msg.format(text)).append(" (").append(url).append(")");
        }
    }
}
