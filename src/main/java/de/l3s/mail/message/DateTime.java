package de.l3s.mail.message;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import de.l3s.learnweb.LanguageBundle;

public class DateTime extends Element {

    private final LocalDateTime dateTime;

    public DateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final LanguageBundle msg) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withLocale(msg.getLocale()).withZone(ZoneId.systemDefault());
        sb.append(formatter.format(dateTime));
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final LanguageBundle msg) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withLocale(msg.getLocale()).withZone(ZoneId.systemDefault());
        sb.append(formatter.format(dateTime));
    }
}
