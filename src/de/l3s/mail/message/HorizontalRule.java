package de.l3s.mail.message;

import de.l3s.learnweb.LanguageBundle;

public class HorizontalRule extends Element {

    @Override
    protected void buildHtml(final StringBuilder sb, final LanguageBundle msg) {
        sb.append("<hr").append(buildAttributes()).append("/>");
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final LanguageBundle msg) {
        sb.append("_____________________________________").append("\n");
    }
}
