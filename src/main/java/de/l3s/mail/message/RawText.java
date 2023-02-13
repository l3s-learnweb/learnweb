package de.l3s.mail.message;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.i18n.MessagesBundle;

public class RawText extends Element {

    private String text;

    public RawText(String text) {
        this.text = text;
    }

    public RawText append(String element) {
        text += element;
        return this;
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final MessagesBundle msg) {
        if (!StringUtils.isAllBlank(getInlineStyle(), getStyleClass())) { // render text inside SPAN element
            sb.append("<span").append(buildAttributes()).append(">");
            sb.append(text);
            sb.append("</span>");
        } else {
            sb.append(text);
        }
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final MessagesBundle msg) {
        sb.append(text);
    }
}
