package de.l3s.learnweb.beans.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;

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
import de.l3s.mail.Mail;
import de.l3s.mail.MailFactory;
import de.l3s.util.bean.BeanHelper;

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
        message.setSenderUser(getUser());
        message.setTitle(this.title);
        message.setText(this.text);
        message.setCreatedAt(LocalDateTime.now());

        int counter = 0;

        ArrayList<String> recipients = new ArrayList<>(selectedUsers.size());
        ArrayList<String> usersWithoutMail = new ArrayList<>();

        for (int userId : selectedUsers) {
            User user = userDao.findByIdOrElseThrow(userId);
            message.setRecipientUser(user);
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
            mail = MailFactory.buildNotificationEmail(title, text, user.getUsername()).build(user.getLocale());
            mail.setReplyTo(user.getEmail());
            mail.setBccRecipients(recipients);
            mail.send();

            addMessage(FacesMessage.SEVERITY_INFO, recipients.size() + " emails send");
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
