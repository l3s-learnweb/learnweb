package de.l3s.learnweb.beans.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.MessageDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.util.bean.BeanHelper;
import de.l3s.util.email.Mail;

@Named
@RequestScoped
public class AdminNotificationBean extends ApplicationBean {
    private static final Logger log = LogManager.getLogger(AdminNotificationBean.class);

    @NotBlank
    private String text;
    @NotBlank
    private String title;
    private TreeNode[] selectedNodes;
    private boolean sendEmail = false; // send the message also per mail
    private boolean moderatorCanSendMail = false;

    private User user;
    private TreeNode treeRoot;

    @Inject
    private UserDao userDao;

    @Inject
    private MessageDao messageDao;

    @PostConstruct
    public void init() {
        user = getUser();
        BeanAssert.authorized(user);
        BeanAssert.hasPermission(user.isModerator());

        if (StringUtils.isNotBlank(user.getEmail())) {
            moderatorCanSendMail = user.isEmailConfirmed();
        }

        treeRoot = BeanHelper.createGroupsUsersTree(user, getLocale(), true);
    }

    public void send() {
        Collection<Integer> selectedUsers = BeanHelper.getSelectedUsers(selectedNodes);
        if (selectedUsers.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Please select the users you want to send a message.");
            return;
        }

        Message message = new Message();
        message.setFromUser(getUser());
        message.setTitle(this.title);
        message.setText(this.text);
        message.setTime(LocalDateTime.now());

        int counter = 0;

        ArrayList<String> recipients = new ArrayList<>(selectedUsers.size());
        ArrayList<String> usersWithoutMail = new ArrayList<>();

        for (int userId : selectedUsers) {
            User user = userDao.findById(userId);
            message.setToUser(user);
            messageDao.save(message);

            if (StringUtils.isEmpty(user.getEmail()) || !user.isEmailConfirmed()) {
                usersWithoutMail.add(user.getUsername());
            } else {
                recipients.add(user.getEmail());
            }
            counter++;
        }

        addMessage(FacesMessage.SEVERITY_INFO, counter + " internal Learnweb notifications sent");

        if (sendEmail && moderatorCanSendMail) {
            sendMail(recipients);

            if (!usersWithoutMail.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_WARN, "Some users haven't defined a valid email address: <b>" + StringUtils.join(usersWithoutMail, ", ") + "</b>");
            }
        }
    }

    private void sendMail(final ArrayList<String> recipients) {
        Mail mail = null;
        try {
            // copy addresses to array
            int i = 0;
            InternetAddress[] recipientsArr = new InternetAddress[recipients.size()];

            for (String address : recipients) {
                recipientsArr[i++] = new InternetAddress(address);
                log.debug("send mail to: {}", address);
            }

            mail = new Mail();
            mail.setRecipients(RecipientType.BCC, recipientsArr);
            mail.setReplyTo(new InternetAddress(user.getEmail()));
            mail.setRecipient(RecipientType.TO, new InternetAddress(user.getEmail()));
            mail.setHTML(text + "<br/>\n<br/>\n___________________________<br/>\n" + getLocaleMessage("mail_notification_footer", user.getUsername()));
            mail.setSubject("Learnweb: " + title);
            mail.sendMail();

            addMessage(FacesMessage.SEVERITY_INFO, recipientsArr.length + " emails send");
        } catch (Exception e) {
            log.error("Could not send notification mail: {}", mail, e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Email could not be sent");
        }
    }

    public TreeNode getTreeRoot() {
        return treeRoot;
    }

    public boolean isModeratorCanSendMail() {
        return moderatorCanSendMail;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(final TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    public boolean isSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(boolean sendEmail) {
        this.sendEmail = sendEmail;
    }
}
