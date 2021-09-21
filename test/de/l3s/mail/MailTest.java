package de.l3s.mail;

import static org.junit.jupiter.api.Assertions.*;

import javax.mail.MessagingException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MailTest {

    @Test
    @Disabled("If enabled, it will send an actual email, I don't want to receive the email every time tests is running")
    void sendMail() throws MessagingException {
        Mail mail = new Mail();
        mail.setRecipient("astappiev@l3s.de");
        mail.setHTML("Hello world!");
        mail.setSubject("Test email");
        mail.send();
        assertTrue(true);
    }
}
