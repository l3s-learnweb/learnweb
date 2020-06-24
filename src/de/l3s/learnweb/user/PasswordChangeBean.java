package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.util.StringHelper;

@Named
@ViewScoped
public class PasswordChangeBean extends ApplicationBean implements Serializable {
    //private static final Logger log = LogManager.getLogger(PasswordChangeBean.class);
    private static final long serialVersionUID = 2237249691332567548L;

    private String parameter;

    private String password;
    private String confirmPassword;

    private User user;

    public void onLoad() throws SQLException {
        BeanAsserts.validateNotEmpty(parameter);
        String[] splits = parameter.split("_");
        BeanAsserts.validate(splits.length == 2 && !StringUtils.isAnyEmpty(splits), "error_pages.bad_request_email_link");

        int userId = StringHelper.parseInt(splits[0], 0);
        String hash = splits[1];

        user = getLearnweb().getUserManager().getUser(userId);
        BeanAsserts.validate(user != null && hash.length() == 32, "error_pages.bad_request_email_link");
        BeanAsserts.validate(hash.equals(PasswordBean.createPasswordChangeHash(user)), "Your request seams to be invalid. Maybe you have already changed the password?");
    }

    public String changePassword() {
        UserManager um = getLearnweb().getUserManager();
        try {
            user.setPassword(password);
            um.save(user);

            setKeepMessages();
            addMessage(FacesMessage.SEVERITY_INFO, "password_changed");
            return "/lw/user/login.xhtml?faces-redirect=true";
        } catch (SQLException e) {
            addErrorMessage(e);
            return null;
        }
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public User getUser() {
        return user;
    }
}
