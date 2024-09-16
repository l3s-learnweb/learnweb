package de.l3s.mail;

import java.io.Serial;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import de.l3s.learnweb.app.ConfigProvider;

@ApplicationScoped
public class MailService implements Serializable {

    @Serial
    private static final long serialVersionUID = -3122617239830631371L;

    @Inject
    private ConfigProvider config;

    public Session createSession() {
        Properties props = new Properties();
        props.setProperty("mail.debug", String.valueOf(config.getPropertyBoolean("smtp_debug", false)));
        props.setProperty("mail.smtp.host", config.getProperty("smtp_host", "localhost"));
        props.setProperty("mail.smtp.port", config.getProperty("smtp_port", "587"));
        props.setProperty("mail.smtp.auth", "true");
        if (config.getPropertyBoolean("smtp_enable_starttls", true)) {
            props.setProperty("mail.smtp.starttls.enable", "true");
        }

        final Authenticator authenticator = new PasswordAuthenticator(
            config.getProperty("smtp_username"),
            config.getProperty("smtp_password")
        );
        return Session.getInstance(props, authenticator);
    }

    public MimeMessage createMessage() {
        try {
            MimeMessage message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(config.getProperty("mail_from_address"), config.getProperty("mail_from_name", "Learnweb"), "UTF-8"));
            return message;
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("The from address is not valid", e);
        }
    }

    public void send(Mail mail) {
        try {
            MimeMessage message = createMessage();
            if (mail.subject != null) {
                message.setSubject(mail.subject);
            }
            if (mail.text != null) {
                message.setText(mail.text);
            }
            if (mail.textHtml != null) {
                message.setText(mail.textHtml, "UTF-8", "html");
            }
            if (!mail.recipients.isEmpty()) {
                for (String address : mail.recipients) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
                }
            }
            if (!mail.recipientsCc.isEmpty()) {
                for (String address : mail.recipientsCc) {
                    message.addRecipient(Message.RecipientType.CC, new InternetAddress(address));
                }
            }
            if (!mail.recipientsBcc.isEmpty()) {
                for (String address : mail.recipientsBcc) {
                    message.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
                }
            }
            if (mail.replyTo != null) {
                InternetAddress[] addresses = {new InternetAddress(mail.replyTo)};
                message.setReplyTo(addresses);
            }

            message.saveChanges();
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Could not send mail", e);
        }
    }
}
