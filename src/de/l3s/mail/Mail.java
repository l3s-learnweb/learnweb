package de.l3s.mail;

import java.util.Collection;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

// TODO move settings
public class Mail {
    public static final String FROM_ADDRESS = "learnweb@kbs.uni-hannover.de";
    public static final Authenticator AUTHENTICATOR = new PasswordAuthenticator("learnweb", "5-FN!@QENtrXh6V][C}*h8-S=yju");

    public static Session createSession() throws MessagingException {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", "mail.kbs.uni-hannover.de");
        props.setProperty("mail.smtp.port", "587");
        props.setProperty("mail.smtp.socketFactory.port", "587");
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.imap.host", "mail.kbs.uni-hannover.de");
        props.setProperty("mail.imap.port", "143");
        props.setProperty("mail.imap.socketFactory.port", "143");
        props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.imap.auth", "true");
        props.setProperty("mail.imap.starttls.enable", "true");
        //props.setProperty("mail.debug", "true");

        return Session.getInstance(props, AUTHENTICATOR);
    }

    private final MimeMessage message;

    public Mail() throws MessagingException {
        message = new MimeMessage(createSession());
        message.setFrom(new InternetAddress(FROM_ADDRESS));
    }

    public void send() throws MessagingException {
        message.saveChanges();
        Transport.send(message);
    }

    public void setReplyTo(String address) throws MessagingException {
        InternetAddress[] addresses = {new InternetAddress(address)};
        message.setReplyTo(addresses);
    }

    public void setRecipient(String address) throws MessagingException {
        message.setRecipient(RecipientType.TO, new InternetAddress(address));
    }

    public void setBccRecipients(final Collection<String> addresses) throws MessagingException {
        int i = 0;
        InternetAddress[] recipientsArr = new InternetAddress[addresses.size()];

        for (String address : addresses) {
            recipientsArr[i++] = new InternetAddress(address);
        }

        message.setRecipients(RecipientType.BCC, recipientsArr);
    }

    public void setSubject(String subject) throws MessagingException {
        message.setSubject(subject);
    }

    public void setText(String text) throws MessagingException {
        message.setText(text);
    }

    public void setHTML(String text) throws MessagingException {
        message.setText(text, "UTF-8", "html");
    }

    public void setContent(Object html, String type) throws MessagingException {
        message.setContent(html, type);
    }

    @Override
    public String toString() {
        return message.toString();
    }

    static class PasswordAuthenticator extends Authenticator {
        private final String username;
        private final String password;

        PasswordAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
