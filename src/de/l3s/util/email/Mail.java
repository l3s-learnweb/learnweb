package de.l3s.util.email;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mail
{
    private static Authenticator authenticator = new PasswordAuthenticator("learnweb", "5-FN!@QENtrXh6V][C}*h8-S=yju");
    private Session session;
    private MimeMessage message;

    public Mail() throws AddressException, MessagingException
    {
        System.setProperty("mail.mime.charset", "UTF-8");
        Properties props = new Properties();
        props.put("mail.smtp.host", "mail.kbs.uni-hannover.de");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        //props.put("mail.debug", "true");

        session = Session.getDefaultInstance(props, authenticator);

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

    public void setRecipient(RecipientType type, InternetAddress adress) throws MessagingException
    {
        message.setRecipient(type, adress);
    }

    public void setRecipients(RecipientType type, InternetAddress[] adresses) throws MessagingException
    {
        message.setRecipients(type, adresses);
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
