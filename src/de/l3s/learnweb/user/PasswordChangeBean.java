package de.l3s.learnweb.user;

import java.io.Serializable;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.util.HashHelper;

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
    private TokenDao tokenDao;

    @Inject
    private RequestManager requestManager;

    public void onLoad() {
        BeanAssert.validate(StringUtils.isNotEmpty(parameter));

        try {
            String[] splits = parameter.split(":");
            Optional<User> userOptional = tokenDao.findUserByToken(Integer.parseInt(splits[0]), HashHelper.sha256(splits[1]));

            if (userOptional.isPresent()) {
                user = userOptional.get();
            } else {
                requestManager.recordFailedAttempt(Faces.getRemoteAddr(), "pass:" + splits[0]);
                throw new BadRequestHttpException("Your request seams to be invalid. Maybe you have already changed the password?");
            }
        } catch (Exception e) {
            throw new BadRequestHttpException("error_pages.bad_request_email_link", e);
        }
    }

    public String changePassword() {
        user.setPassword(password);
        tokenDao.getUserDao().save(user);
        tokenDao.deleteByTypeAndUser(Token.TokenType.PASSWORD_RESET, user.getId());

        setKeepMessages();
        addMessage(FacesMessage.SEVERITY_INFO, "password_changed");
        return "/lw/user/login.xhtml?faces-redirect=true";
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
