package de.l3s.mail.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.mail.MessagingException;

import org.apache.solr.common.StringUtils;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.mail.Mail;

public class MessageBuilder {

    private String subject;
    private String headStyle;
    private final java.util.List<Element> bodyElements = new ArrayList<>();
    private final java.util.List<Element> footerElements = new ArrayList<>();

    public MessageBuilder() {
    }

    public MessageBuilder(final String subject) {
        this.subject = subject;
    }

    public MessageBuilder add(Element element) {
        bodyElements.add(element);
        return this;
    }

    public MessageBuilder headStyle(String style) {
        headStyle = style;
        return this;
    }

    public MessageBuilder footer(Element... elem) {
        if (elem != null) {
            Collections.addAll(footerElements, elem);
        }
        return this;
    }

    public MessageBuilder defaultFooter() {
        return footer(new HorizontalRule(), new Text("email.regards"), new LineBreak(), new Text("email.team"));
    }

    public String getSubject(final ResourceBundle msg) {
        return msg.getString(subject);
    }

    public String buildHtmlText(final ResourceBundle msg) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<head>");
        sb.append("<meta charset=\"UTF-8\">");
        if (!StringUtils.isEmpty(headStyle)) {
            sb.append("<style type=\"text/css\">").append(headStyle).append("</style>");
        }
        sb.append("</head>");
        sb.append("<body>");
        for (Element element : bodyElements) {
            element.buildHtml(sb, msg);
        }

        if (!footerElements.isEmpty()) {
            sb.append("<footer>");
            for (Element element : footerElements) {
                element.buildHtml(sb, msg);
            }
            sb.append("</footer>");
        }

        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }

    public String buildPlainText(final ResourceBundle msg) {
        StringBuilder sb = new StringBuilder();
        for (Element element : bodyElements) {
            element.buildPlainText(sb, msg);
        }
        for (Element element : footerElements) {
            element.buildPlainText(sb, msg);
        }
        return sb.toString();
    }

    public Mail build(Locale locale) throws MessagingException {
        ResourceBundle bundle = LanguageBundle.getLanguageBundle(locale);

        Mail mail = new Mail();
        mail.setSubject(getSubject(bundle));
        mail.setText(buildPlainText(bundle));
        mail.setHTML(buildHtmlText(bundle));
        return mail;
    }
}
