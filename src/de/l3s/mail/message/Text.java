package de.l3s.mail.message;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.LanguageBundle;

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
    protected void buildHtml(final StringBuilder sb, final LanguageBundle msg) {
        if (!StringUtils.isAllBlank(getInlineStyle(), getStyleClass())) { // render text inside SPAN element
            sb.append("<span").append(buildAttributes()).append(">");
            sb.append(msg.getFormatted(text, objects));
            sb.append("</span>");
        } else {
            sb.append(msg.getFormatted(text, objects));
        }
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final LanguageBundle msg) {
        sb.append(msg.getFormatted(text, objects));
    }
}