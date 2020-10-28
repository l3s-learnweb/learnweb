package de.l3s.util.email;

import static org.junit.jupiter.api.Assertions.*;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;

class MailTest {

    @Test
    void sendMail() throws MessagingException {
        Mail mail = new Mail();
        mail.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress("astappiev@l3s.de"));
        mail.setHTML("Hello world!");
        mail.setSubject("Test email");
        mail.sendMail();
        assertTrue(true);
    }
}
