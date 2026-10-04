package de.l3s.mail.message;

import java.util.Map;

import org.apache.commons.lang3.Validate;

import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.util.StringHelper;

public class Link extends Element {
    private final String text;
    private final String url;
    private final boolean plainText; // the text is not a message key but plain text, e.g. a title

    public Link(String url) {
        Validate.notBlank(url);
        this.text = null;
        this.url = url;
        this.plainText = false;
    }

    /**
     * @param text a message key
     */
    public Link(String url, String text) {
        this(url, text, false);
    }

    private Link(String url, String text, boolean plainText) {
        Validate.notBlank(url);
        Validate.notBlank(text);
        this.text = text;
        this.url = url;
        this.plainText = plainText;
    }

    /**
     * @param text plain text which is not translated, e.g. a title
     */
    public static Link withPlainText(String url, String text) {
        return new Link(url, text, true);
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final MessagesBundle msg) {
        sb.append("<a").append(buildAttributes(Map.of("href", url))).append(">");
        if (null == text) {
            sb.append(StringHelper.escapeHtml(url));
        } else {
            sb.append(plainText ? StringHelper.escapeHtml(text) : msg.format(text));
        }
        sb.append("</a>");
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final MessagesBundle msg) {
        if (text == null) {
            sb.append(url);
        } else {
            sb.append(plainText ? text : msg.format(text)).append(" (").append(url).append(")");
        }
    }
}
