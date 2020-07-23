package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;

@Named
@RequestScoped
public class ConfirmEmailBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -6040579499279231182L;

    private String email;
    private String token;

    private User user;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    public String onLoad() throws SQLException {
        BeanAssert.validate(!StringUtils.isAnyEmpty(email, token), "error_pages.bad_request_email_link");
        BeanAssert.validate(token.length() >= 32, "confirm_token_to_short");

        user = getLearnweb().getUserManager().getUserByEmailAndConfirmationToken(email, token);
        BeanAssert.validate(user, "confirm_token_invalid");

        user.setEmailConfirmed(true);
        user.save();

        if (user.equals(getConfirmRequiredBean().getLoggedInUser())) {
            LoginBean.loginUser(this, user);
            return user.getOrganisation().getWelcomePage() + "?faces-redirect=true";
        }
        return null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isConfirmed() {
        return user != null && user.isEmailConfirmed();
    }

    @Override
    public User getUser() {
        return user;
    }

    public ConfirmRequiredBean getConfirmRequiredBean() {
        return confirmRequiredBean;
    }

    public void setConfirmRequiredBean(ConfirmRequiredBean confirmRequiredBean) {
        this.confirmRequiredBean = confirmRequiredBean;
    }
}
