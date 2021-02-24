package de.l3s.learnweb.beans.publicPages;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;
import de.l3s.util.bean.BeanHelper;
import de.l3s.util.email.Mail;

@Named
@RequestScoped
public class ContactBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 1506604546829332647L;

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
            Mail message = new Mail();
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(adminEmail));
            message.setSubject("Contact Form message");
            message.setText("name: " + name + "\nemail: " + email + "\nmessage: " + this.message + "\n\n-------\n" + BeanHelper.getRequestSummary());
            message.sendMail();

            addMessage(FacesMessage.SEVERITY_INFO, "Request sent successfully. We will get back to you shortly.");
            clearForm();
        } catch (MessagingException mex) {
            addErrorMessage(mex);
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
