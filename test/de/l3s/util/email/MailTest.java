package de.l3s.util.email;

import static org.junit.jupiter.api.Assertions.*;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class MailTest {

    @Test
    void sendMail() throws MessagingException {
        Mail mail = new Mail();
        mail.setRecipient(RecipientType.TO, new InternetAddress("astappiev@l3s.de"));
        mail.setHTML("Hello world!");
        mail.setSubject("Test email");
        mail.sendMail();
        assertTrue(true);
    }
}
