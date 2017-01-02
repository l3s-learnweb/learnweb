package de.l3s.learnweb.beans.admin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;

import de.l3s.learnweb.Message;
import de.l3s.learnweb.User;
import de.l3s.learnweb.UserManager;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.util.Mail;
import de.l3s.util.StringHelper;

@ManagedBean
@RequestScoped
public class AdminNotificationBean extends ApplicationBean
{
    private static final Logger log = Logger.getLogger(AdminNotificationBean.class);
    @NotEmpty
    private String text;
    @NotEmpty
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
            moderatorCanSendMail = validator.isValid(user.getEmail(), null);
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
        TreeSet<Integer> selectedUsers = new TreeSet<Integer>();
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

        ArrayList<String> reciepients = new ArrayList<>(selectedUsers.size());
        ArrayList<String> usersWithoutMail = new ArrayList<>();

        for(int userId : selectedUsers)
        {
            User user = um.getUser(userId);
            message.setToUser(user);
            message.save();

            if(sendEmail)
            {
                log.debug("try send mail to: " + user.getEmail());

                if(StringUtils.isEmpty(user.getEmail()) || !validator.isValid(user.getEmail(), null))
                    usersWithoutMail.add(user.getUsername());
                else
                    reciepients.add(user.getEmail());
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
                InternetAddress[] reciepientsArr = new InternetAddress[reciepients.size()];

                for(String address : reciepients)
                {
                    reciepientsArr[i++] = new InternetAddress(address);
                    log.debug("send mail to: " + address);
                }

                mail = new Mail();
                mail.setRecipients(javax.mail.Message.RecipientType.BCC, reciepientsArr);
                mail.setReplyTo(new InternetAddress(user.getEmail()));
                mail.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(user.getEmail()));
                mail.setHTML(text + "<br/>\n<br/>\n___________________________<br/>\n" + getLocaleMessage("mail_notification_footer", user.getUsername()));
                mail.setSubject("Learnweb: " + title);
                mail.sendMail();

                addMessage(FacesMessage.SEVERITY_INFO, reciepientsArr.length + " emails send");

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

    //Alana
    public void send2() throws SQLException
    {
        log.debug("Send2");
        if(null == this.listStudents || this.listStudents.length == 0)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "Please select the users you want to send a message.");
            return;
        }

        TreeSet<Integer> selectedUsers = new TreeSet<Integer>();
        for(String userId : this.listStudents)
        {
            selectedUsers.add(Integer.parseInt(userId));
        }

        User fromUser = getUser();

        Message message = new Message();
        message.setFromUser(fromUser);
        message.setTitle(this.title);
        message.setText(this.text);
        message.setTime(new Date());

        UserManager um = getLearnweb().getUserManager();
        int counter = 0;

        for(int userId : selectedUsers)
        {
            User user = um.getUser(userId);
            message.setToUser(user);
            message.save();

            counter++;
        }
        addMessage(FacesMessage.SEVERITY_INFO, counter + " Notifications send");
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

    //Alana
    public String[] getListStudents()
    {
        return listStudents;
    }

    public void setListStudents(String[] listStudents)
    {
        this.listStudents = listStudents;
    }
}
