package de.l3s.mail.message;

import de.l3s.learnweb.LanguageBundle;

public class LineBreak extends Element {
    @Override
    protected void buildHtml(final StringBuilder sb, final LanguageBundle msg) {
        sb.append("<br/>"); // style and class attributes are almost useless for BR elements -> not rendered
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final LanguageBundle msg) {
        sb.append("\n");
    }
}
