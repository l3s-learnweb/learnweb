package de.l3s.mail;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.jboss.weld.junit5.auto.ExcludeBean;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.l3s.learnweb.app.ConfigProvider;

@EnableAutoWeld
@AddPackages(MailService.class)
class MailTest {

    @Produces
    @ExcludeBean // Excludes beans with type ConfigProvider from automatic discovery
    ConfigProvider config = new ConfigProvider(false);

    @Inject
    MailService mailService;

    @Test
    @Disabled("If enabled, it will send an actual email, I don't want to receive the email every time tests is running")
    void sendMail() {
        Mail mail = new Mail();
        mail.addRecipient("astappiev@l3s.de");
        mail.setHTML("Hello world!");
        mail.setSubject("Test email2");
        mailService.send(mail);
        assertTrue(true);
    }
}
