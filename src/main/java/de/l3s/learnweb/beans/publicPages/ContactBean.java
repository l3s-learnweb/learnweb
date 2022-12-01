package de.l3s.learnweb.beans.publicPages;

import java.io.Serial;
import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.user.User;
import de.l3s.mail.Mail;
import de.l3s.mail.MailFactory;

@Named
@RequestScoped
public class ContactBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1506604546829332647L;
    private static final Logger log = LogManager.getLogger(ContactBean.class);

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String message;

    @PostConstruct
    public void init() {
        User user = getUser();

        if (null != user) { // set default value
            name = user.getUsername();
            email = user.getEmail();
        }
    }

    public void sendMail() {
        String adminEmail = config().getProperty("admin_mail");

        try {
            Mail mail = MailFactory.buildContactFormEmail(name, email, message).build(getLocale());
            mail.setRecipient(adminEmail);
            mail.setReplyTo(email);
            mail.send();

            log.error("Contact form mail sent to {}", adminEmail); // required to collect more data about the user
            clearForm();
        } catch (MessagingException e) {
            throw new HttpException("Failed to send email", e);
        }
    }

    public void clearForm() {
        name = null;
        email = null;
        message = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static String encryptMailAddress(String address) {
        if (StringUtils.isEmpty(address)) {
            return "";
        }

        return address.replace(".", ".<span class=\"d-none\">[remove me]</span>");
    }

}
