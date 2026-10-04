package de.l3s.mail.message;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.util.StringHelper;

/**
 * A message key that is translated, string arguments are escaped in the HTML representation.
 */
public class Text extends Element {

    private String text;
    private Object[] textArgs;

    public Text(String text) {
        this.text = text;
    }

    public Text(String text, Object... args) {
        this.text = text;
        this.textArgs = args;
    }

    public Text append(String element) {
        text += element;
        return this;
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final MessagesBundle msg) {
        if (!StringUtils.isAllBlank(getInlineStyle(), getStyleClass())) { // render text inside SPAN element
            sb.append("<span").append(buildAttributes()).append(">");
            sb.append(msg.format(text, escapeArgs()));
            sb.append("</span>");
        } else {
            sb.append(msg.format(text, escapeArgs()));
        }
    }

    private Object[] escapeArgs() {
        return textArgs == null ? null : Arrays.stream(textArgs).map(arg -> arg instanceof CharSequence str ? StringHelper.escapeHtml(str.toString()) : arg).toArray();
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final MessagesBundle msg) {
        sb.append(msg.format(text, textArgs));
    }
}
