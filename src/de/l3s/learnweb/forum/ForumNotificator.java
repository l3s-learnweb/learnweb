package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.User.NotificationFrequency;
import de.l3s.util.SHA512;
import de.l3s.util.email.Mail;

public class ForumNotificator implements Runnable, Serializable
{
    private static final long serialVersionUID = -7141107765791779330L;
    private static final Logger log = LogManager.getLogger(ForumNotificator.class);

    public ForumNotificator()
    {
    }

    @Override
    public void run()
    {
        try
        {
            Calendar cal = Calendar.getInstance();
            ForumManager forumManager = Learnweb.getInstance().getForumManager();
            List<User> users = Learnweb.getInstance().getUserManager().getUsers();
            for(User user : users)
            {
                if(!user.getPreferredNotificationFrequency().equals(NotificationFrequency.NEVER))
                {
                    // get changes of all groups for which the user has selected daily notifications
                    List<ForumTopic> topicsPerPeriod = forumManager.getTopicByPeriod(user.getId(), NotificationFrequency.DAILY);

                    // On every Sunday: get changes of all groups for which the user has selected weekly notifications
                    if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    {
                        topicsPerPeriod.addAll(forumManager.getTopicByPeriod(user.getId(), NotificationFrequency.WEEKLY));
                    }

                    // On first day of every month: get changes of all groups for which the user has selected monthly notifications
                    if(cal.get(Calendar.DAY_OF_MONTH) == 1)
                    {
                        topicsPerPeriod.addAll(forumManager.getTopicByPeriod(user.getId(), NotificationFrequency.MONTHLY));
                    }

                    if(!topicsPerPeriod.isEmpty())
                        sendMailWithNewTopics(user, topicsPerPeriod);
                }
            }
        }
        catch(Throwable e)
        {
            log.error("Error while creating forum notifications", e);
        }
    }

    public static String getHash(User user)
    {
        return user.getId() + ":" + SHA512.hash(Learnweb.salt1 + user.getId() + Learnweb.salt2 + "notification");
    }

    private void sendMailWithNewTopics(User user, List<ForumTopic> topics) throws MessagingException, SQLException
    {
        List<ForumTopic> userTopics = topics.stream().filter(topic -> topic.getUserId() == user.getId()).collect(Collectors.toList());
        List<ForumTopic> otherTopics = topics.stream().filter(topic -> topic.getUserId() != user.getId()).collect(Collectors.toList());

        // load translations for users preferred language
        ResourceBundle msg = LanguageBundle.getLanguageBundle(user.getLocale());

        Mail mail = new Mail();
        mail.setSubject(msg.getString("email_forum_notifications.email_subject"));
        mail.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));

        StringBuilder content = new StringBuilder();

        content.append("<html><head><style type=\"text/css\">" +
                "  table{" +
                "    border-collapse: collapse;\n" +
                "    width:100%;" +
                "  }" +
                "  th{" +
                "    border: 1px solid #dddddd;" +
                "    text-align: left;" +
                "    padding: 8px;" +
                "  }" +
                "  td{" +
                "    border: 1px solid #dddddd;" +
                "    text-align: left;" +
                "    padding: 8px;" +
                "  }" +
                "  .first-child{" +
                "    width:20%;" +
                "    max-width: 0;" +
                "    white-space: nowrap;" +
                "    overflow: hidden;" +
                "    text-overflow: ellipsis;" +
                "  }" +
                "  .second-child{" +
                "    max-width: 0;" +
                "    white-space: nowrap;" +
                "    overflow: hidden;" +
                "    text-overflow: ellipsis;" +
                "    word-wrap:break-word;" +
                "  }" +
                "  .third-child{" +
                "    width:15%;" +
                "    word-wrap:break-word;" +
                "  }" +
                "  ul{" +
                "    padding-left:0;" +
                "  }" +
                "  h4{" +
                "    margin-bottom:0;" +
                "  }" +
                "</style></head><body>");
        content.append("<span>").append(msg.getString("greeting")).append(" ").append(user.getUsername()).append("</span>").append("<br>");
        content.append("<span>").append(msg.getString("email_forum_notifications.resent_updates")).append("</span>");

        if(!userTopics.isEmpty())
        {
            content.append("<br>");
            content.append("<h4>").append(msg.getString("email_forum_notifications.new_answers_to_your_posts")).append("</h4>");
            content.append("<table>");
            content.append("  <tr>\n" +
                    "    <th>" + msg.getString("group") + "</th>\n" +
                    "    <th>" + msg.getString("title") + "</th>\n" +
                    "    <th>" + msg.getString("last_activities") + "</th>\n" +
                    "  </tr>");
            for(ForumTopic topic : userTopics)
            {
                content.append("<tr>");

                content.append("<td class=\"first-child\">");
                content.append("\n<a href='" + Learnweb.getInstance().getServerUrl() + "/lw/group/overview.jsf?group_id=").append(topic.getGroupId()).append("'>").append(Learnweb.getInstance().getGroupManager().getGroupById(topic.getGroupId()).getTitle()).append("</a>");
                content.append("</td>");

                content.append("<td class=\"second-child\">");
                content.append("\n<a href='" + Learnweb.getInstance().getServerUrl() + "/lw/group/forum_post.jsf?topic_id=").append(topic.getId()).append("'>").append(topic.getTitle()).append("</a>");
                content.append("</td>");

                content.append("<td class=\"third-child\">");
                content.append(MessageFormat.format("{0, date, medium} {1, time, short}", topic.getLastPostDate(), topic.getLastPostDate()));
                content.append("</td>");

                content.append("</tr>");
            }
            content.append("</table>");
        }
        if(!otherTopics.isEmpty())
        {
            content.append("<br>");
            content.append("<h4>").append(msg.getString("email_forum_notifications.other_new_posts")).append("</h4>");
            content.append("<table>");
            content.append("  <tr>\n" +
                    "    <th>" + msg.getString("group") + "</th>\n" +
                    "    <th>" + msg.getString("title") + "</th>\n" +
                    "    <th>" + msg.getString("last_activities") + "</th>\n" +
                    "  </tr>");
            for(ForumTopic topic : otherTopics)
            {
                content.append("<tr>");

                content.append("<td class=\"first-child\">");
                content.append("\n<a href='" + Learnweb.getInstance().getServerUrl() + "/lw/group/overview.jsf?group_id=").append(topic.getGroupId()).append("'>").append(Learnweb.getInstance().getGroupManager().getGroupById(topic.getGroupId()).getTitle()).append("</a>");
                content.append("</td>");

                content.append("<td class=\"second-child\">");
                content.append("\n<a href='" + Learnweb.getInstance().getServerUrl() + "/lw/group/forum_post.jsf?topic_id=").append(topic.getId()).append("'>").append(topic.getTitle()).append("</a>");
                content.append("</td>");

                content.append("<td class=\"third-child\">");
                content.append(MessageFormat.format("{0, date, medium} {1, time, short}", topic.getLastPostDate(), topic.getLastPostDate()));
                content.append("</td>");

                content.append("</tr>");
            }
            content.append("</table>");
        }

        content.append("<ul>");
        content.append("<li>");
        content.append("<a href='" + Learnweb.getInstance().getServerUrl() + "/lw/myhome/profile.jsf?user_id=").append(user.getId()).append("'>").append(msg.getString("email_forum_notifications.change_how_often_we_send_mails")).append("</a>.");
        content.append("</li>");
        content.append("<li>");
        content.append("<a href='" + Learnweb.getInstance().getServerUrl() + "/lw/user/unsubscribe.jsf?hash=").append(getHash(user)).append("'>").append(msg.getString("notification_settings.unsubscribe_from_all")).append("</a>.");
        content.append("</li>");
        content.append("</ul>");
        content.append("</body></html>");

        mail.setHTML(content.toString());
        mail.sendMail();
    }
}
