package de.l3s.mail.message;

import java.util.ResourceBundle;

public class LineBreak extends Element {
    @Override
    protected void buildHtml(final StringBuilder sb, final ResourceBundle msg) {
        sb.append("<br/>"); // style and class attributes are almost useless for BR elements -> not rendered
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final ResourceBundle msg) {
        sb.append("\n");
    }
}
