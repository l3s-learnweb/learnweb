package de.l3s.learnweb.user;

import java.io.Serializable;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.Email;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@SessionScoped
public class ConfirmRequiredBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 934105342636869805L;

    private User loggedInUser;

    @Email
    private String email;

    @Inject
    private UserDao userDao;

    @Override
    public User getUser() {
        if (super.getUser() != null) {
            return super.getUser();
        }

        return loggedInUser;
    }

    public boolean isConfirmed() {
        return getUser() != null && getUser().isEmailConfirmed();
    }

    public void onSubmitNewEmail() {
        User user = getUser();
        if (StringUtils.isNotEmpty(email) && !StringUtils.equals(user.getEmail(), email)) {
            user.setEmail(email);
            userDao.save(user);
        }

        if (user.sendEmailConfirmation()) {
            addMessage(FacesMessage.SEVERITY_INFO, "email_has_been_sent");
        } else {
            addMessage(FacesMessage.SEVERITY_FATAL, "We were not able to send a confirmation mail");
        }
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
        this.email = loggedInUser.getEmail();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
