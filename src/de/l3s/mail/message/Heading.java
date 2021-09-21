package de.l3s.mail.message;

import java.util.ResourceBundle;

public class Heading extends Element {

    private final int size;
    private final String text;

    public Heading(int size, String text) {
        this.size = size;
        this.text = text;
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final ResourceBundle msg) {
        sb.append("<h").append(size).append(buildAttributes()).append(">");
        sb.append(msg.getString(text));
        sb.append("</h").append(size).append(">");
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final ResourceBundle msg) {
        sb.append(msg.getString(text)).append("\n");
    }
}
