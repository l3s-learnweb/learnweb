package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.exceptions.BadRequestHttpException;

@Named
@RequestScoped
public class ConfirmEmailBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -6040579499279231182L;

    private String email;
    private String token;

    private User user;

    @Inject
    private TokenDao tokenDao;
    @Inject
    private UserDao userDao;

    @Inject
    private ConfirmRequiredBean confirmRequiredBean;

    public String onLoad() {
        BeanAssert.validate(!StringUtils.isAnyEmpty(email, token), "error_pages.bad_request_email_link");
        String[] splits = token.split(":");
        BeanAssert.validate(splits.length == 2 && !StringUtils.isAnyEmpty(splits), "error_pages.bad_request_email_link");

        try {
            user = tokenDao.findUserByToken(Integer.parseInt(splits[0]), splits[1]).orElseThrow();
            BeanAssert.validate(user.getEmail().equals(email), "confirm_token_invalid");

            user.setEmailConfirmed(true);
            tokenDao.getUserDao().save(user);
            tokenDao.deleteByTypeAndUser(Token.TokenType.EMAIL_CONFIRMATION, user.getId());

            if (user.equals(confirmRequiredBean.getLoggedInUser())) {
                LoginBean.loginUser(this, user);
                return "/lw/" + user.getOrganisation().getWelcomePage() + "?faces-redirect=true";
            }
            return null;
        } catch (Exception e) {
            // try one more attempt with email, if user already used the token
            List<User> users = userDao.findByEmail(email);
            if (!users.isEmpty() && users.stream().anyMatch(User::isEmailConfirmed)) {
                // redirect to welcome page (a login form is shown there, if user is not logged in)
                return "/lw/" + users.get(0).getOrganisation().getWelcomePage() + "?faces-redirect=true";
            }
            throw new BadRequestHttpException("confirm_token_invalid", e);
        }
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
}
