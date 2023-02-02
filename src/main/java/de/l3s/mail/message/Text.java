package de.l3s.mail.message;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.i18n.MessagesBundle;

public class Text extends Element {

    private String text;
    private Object[] objects;

    public Text(String text) {
        this.text = text;
    }

    public Text(String text, Object... objects) {
        this.text = text;
        this.objects = objects;
    }

    public Text append(String element) {
        text += element;
        return this;
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final MessagesBundle msg) {
        if (!StringUtils.isAllBlank(getInlineStyle(), getStyleClass())) { // render text inside SPAN element
            sb.append("<span").append(buildAttributes()).append(">");
            sb.append(msg.format(text, objects));
            sb.append("</span>");
        } else {
            sb.append(msg.format(text, objects));
        }
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final MessagesBundle msg) {
        sb.append(msg.format(text, objects));
    }
}
