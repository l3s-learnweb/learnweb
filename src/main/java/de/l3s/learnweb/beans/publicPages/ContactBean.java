package de.l3s.learnweb.beans.publicPages;

import java.io.Serial;
import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.user.User;
import de.l3s.mail.Mail;
import de.l3s.mail.MailFactory;
import de.l3s.mail.MailService;

@Named
@RequestScoped
public class ContactBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1506604546829332647L;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String message;

    @Inject
    private MailService mailService;

    @PostConstruct
    public void init() {
        User user = getUser();

        if (null != user) { // set default value
            name = user.getUsername();
            email = user.getEmail();
        }
    }

    public void sendMail() {
        try {
            Mail mail = MailFactory.buildContactFormEmail(name, email, message).build(getLocale());
            mail.addRecipient(config().getSupportEmail());
            mail.setReplyTo(email);
            mailService.send(mail);

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
}
