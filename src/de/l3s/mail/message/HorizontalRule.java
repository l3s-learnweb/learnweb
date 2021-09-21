package de.l3s.mail.message;

import java.util.ResourceBundle;

public class HorizontalRule extends Element {

    @Override
    protected void buildHtml(final StringBuilder sb, final ResourceBundle msg) {
        sb.append("<hr").append(buildAttributes()).append("/>");
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final ResourceBundle msg) {
        sb.append("_____________________________________").append("\n");
    }
}
