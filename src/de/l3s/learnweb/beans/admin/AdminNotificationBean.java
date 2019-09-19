package de.l3s.learnweb.beans.admin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Message;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.StringHelper;
import de.l3s.util.email.EmailValidator;
import de.l3s.util.email.Mail;

@Named
@RequestScoped
public class AdminNotificationBean extends ApplicationBean
{
    private static final Logger log = Logger.getLogger(AdminNotificationBean.class);
    @NotBlank
    private String text;
    @NotBlank
    private String title;
    private boolean sendEmail = false; // send the message also per mail
    private boolean moderatorCanSendMail = false;
    //Alana
    private String[] listStudents;
    private User user;
    private EmailValidator validator = new EmailValidator();

    public AdminNotificationBean()
    {
        user = getUser();
        if(user == null || !user.isModerator())
            return;

        if(StringUtils.isNotBlank(user.getEmail()))
            moderatorCanSendMail = validator.isValid(user.getEmail());
    }

    public void send() throws SQLException
    {
        // get selected users, complicated because jsf sucks
        HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
        String[] tempSelectedUsers = request.getParameterValues("selected_users");

        if(null == tempSelectedUsers || tempSelectedUsers.length == 0)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "Please select the users you want to send a message.");
            return;
        }

        // Set is used to make sure that every user gets the message only once
        TreeSet<Integer> selectedUsers = new TreeSet<>();
        for(String userId : tempSelectedUsers)
        {
            selectedUsers.add(Integer.parseInt(userId));
        }

        Message message = new Message();
        message.setFromUser(getUser());
        message.setTitle(this.title);
        message.setText(this.text);
        message.setTime(new Date());

        UserManager um = getLearnweb().getUserManager();
        int counter = 0;

        ArrayList<String> recipients = new ArrayList<>(selectedUsers.size());
        ArrayList<String> usersWithoutMail = new ArrayList<>();

        for(int userId : selectedUsers)
        {
            User user = um.getUser(userId);
            message.setToUser(user);
            message.save();

            if(sendEmail)
            {
                log.debug("try send mail to: " + user.getEmail());

                if(StringUtils.isEmpty(user.getEmail()) || !validator.isValid(user.getEmail()))
                    usersWithoutMail.add(user.getUsername());
                else
                    recipients.add(user.getEmail());
            }
            counter++;
        }

        addMessage(FacesMessage.SEVERITY_INFO, counter + " internal Learnweb notifications sent");

        if(sendEmail && moderatorCanSendMail)
        {
            Mail mail = null;
            try
            {
                // copy addresses to array
                int i = 0;
                InternetAddress[] recipientsArr = new InternetAddress[recipients.size()];

                for(String address : recipients)
                {
                    recipientsArr[i++] = new InternetAddress(address);
                    log.debug("send mail to: " + address);
                }

                mail = new Mail();
                mail.setRecipients(javax.mail.Message.RecipientType.BCC, recipientsArr);
                mail.setReplyTo(new InternetAddress(user.getEmail()));
                mail.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(user.getEmail()));
                mail.setHTML(text + "<br/>\n<br/>\n___________________________<br/>\n" + getLocaleMessage("mail_notification_footer", user.getUsername()));
                mail.setSubject("Learnweb: " + title);
                mail.sendMail();

                addMessage(FacesMessage.SEVERITY_INFO, recipientsArr.length + " emails send");

                if(usersWithoutMail.size() > 0)
                    addMessage(FacesMessage.SEVERITY_WARN, "Some users haven't defined a valid mail address: <b>" + StringHelper.implode(usersWithoutMail, ", ") + "</b>");
            }
            catch(Exception e)
            {
                log.error("Could not send notification mail: " + mail, e);
                addMessage(FacesMessage.SEVERITY_ERROR, "Email could not be sent");
            }

        }

    }

    public boolean isModeratorCanSendMail()
    {
        return moderatorCanSendMail;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public boolean isSendEmail()
    {
        return sendEmail;
    }

    public void setSendEmail(boolean sendEmail)
    {
        this.sendEmail = sendEmail;
    }

    public void setListStudents(String[] listStudents)
    {
        this.listStudents = listStudents;
    }
}
