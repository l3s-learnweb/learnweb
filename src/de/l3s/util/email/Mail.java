package de.l3s.util.email;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mail
{
    private static Authenticator authenticator = new PasswordAuthenticator("learnweb", "5-FN!@QENtrXh6V][C}*h8-S=yju");
    private Session session;
    private MimeMessage message;

    public Mail() throws MessagingException
    {
        System.setProperty("mail.mime.charset", "UTF-8");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        Properties props = new Properties();
        props.put("mail.smtp.host", "mail.kbs.uni-hannover.de");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        session = Session.getInstance(props, authenticator);

        message = new MimeMessage(session);
        message.setFrom(new InternetAddress("learnweb@kbs.uni-hannover.de"));
    }

    public void sendMail() throws MessagingException
    {
        message.saveChanges();
        Transport.send(message);
    }

    public void setReplyTo(InternetAddress internetAddress) throws MessagingException
    {
        InternetAddress[] addresses = { internetAddress };
        message.setReplyTo(addresses);
    }

    public void setFrom(InternetAddress internetAddress) throws MessagingException
    {
        message.setFrom(internetAddress);
    }

    public void setRecipient(RecipientType type, InternetAddress address) throws MessagingException
    {
        message.setRecipient(type, address);
    }

    public void setRecipient(RecipientType type, String address) throws MessagingException
    {
        setRecipient(type, new InternetAddress(address));
    }

    public void setRecipients(RecipientType type, InternetAddress[] addresses) throws MessagingException
    {
        message.setRecipients(type, addresses);
    }

    public void setSubject(String subject) throws MessagingException
    {
        message.setSubject(subject);
    }

    public void setText(String text) throws MessagingException
    {
        message.setText(text);
    }

    public void setHTML(String text) throws MessagingException
    {
        message.setText(text, "UTF-8", "html");
    }

    @Override
    public String toString()
    {
        return message.toString();
    }
}
