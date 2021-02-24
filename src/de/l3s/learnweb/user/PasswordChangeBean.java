package de.l3s.learnweb.user;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;

@Named
@ViewScoped
public class PasswordChangeBean extends ApplicationBean implements Serializable {
    //private static final Logger log = LogManager.getLogger(PasswordChangeBean.class);
    private static final long serialVersionUID = 2237249691332567548L;

    private String parameter;

    private String password;
    private String confirmPassword;

    private User user;

    @Inject
    private UserDao userDao;

    public void onLoad() {
        BeanAssert.validate(StringUtils.isNotEmpty(parameter));
        String[] splits = parameter.split("_");
        BeanAssert.validate(splits.length == 2 && !StringUtils.isAnyEmpty(splits), "error_pages.bad_request_email_link");

        int userId = NumberUtils.toInt(splits[0]);
        String hash = splits[1];

        user = userDao.findById(userId);
        BeanAssert.validate(user != null && hash.length() == PasswordBean.PASSWORD_CHANGE_HASH_LENGTH, "error_pages.bad_request_email_link");
        BeanAssert.validate(hash.equals(PasswordBean.createPasswordChangeHash(user)), "Your request seams to be invalid. Maybe you have already changed the password?");
    }

    public String changePassword() {
        user.setPassword(password);
        userDao.save(user);

        setKeepMessages();
        addMessage(FacesMessage.SEVERITY_INFO, "password_changed");
        return "user/login.xhtml?faces-redirect=true";
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
