package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.mail.Mail;
import de.l3s.mail.MailFactory;
import de.l3s.mail.MailService;
import de.l3s.util.StringHelper;

@RequestScoped
public class EmailConfirmationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -1427221214029851844L;
    private static final Logger log = LogManager.getLogger(EmailConfirmationBean.class);

    @Inject
    private TokenDao tokenDao;

    @Inject
    private MailService mailService;

    /**
     * @return FALSE if an error occurred while sending this message
     */
    public boolean sendEmailConfirmation(User user) {
        try {
            String token = RandomStringUtils.secure().nextAlphanumeric(32);
            int tokenId = tokenDao.override(user.getId(), Token.TokenType.EMAIL_CONFIRMATION, token, LocalDateTime.now().plusYears(1));

            String confirmEmailUrl = Learnweb.config().getServerUrl() + "/lw/user/confirm_email.jsf?" +
                "email=" + StringHelper.urlEncode(user.getEmail()) + "&token=" + tokenId + ":" + token;

            Mail mail = MailFactory.buildConfirmEmail(user.getUsername(), confirmEmailUrl).build(user.getLocale());
            mail.addRecipient(user.getEmail());
            mailService.send(mail);
            return true;
        } catch (MessagingException e) {
            log.error("Can't send confirmation mail to {}", this, e);
        }
        return false;
    }
}
