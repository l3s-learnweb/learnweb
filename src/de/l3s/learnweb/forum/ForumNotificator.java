package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.User.NotificationFrequency;
import de.l3s.learnweb.user.UserDao;
import de.l3s.util.HashHelper;
import de.l3s.util.email.Mail;

public class ForumNotificator implements Runnable, Serializable {
    private static final long serialVersionUID = -7141107765791779330L;
    private static final Logger log = LogManager.getLogger(ForumNotificator.class);

    @Inject
    private UserDao userDao;

    @Inject
    private ForumTopicDao forumTopicDao;

    @Override
    public void run() {
        try {
            LocalDate localDate = LocalDate.now();

            ArrayList<NotificationFrequency> frequencies = new ArrayList<>(3);
            frequencies.add(NotificationFrequency.DAILY);

            // On every Sunday: get changes of all groups for which the user has selected weekly notifications
            if (localDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                frequencies.add(NotificationFrequency.WEEKLY);
            }

            // On first day of every month: get changes of all groups for which the user has selected monthly notifications
            if (localDate.getDayOfMonth() == 1) {
                frequencies.add(NotificationFrequency.MONTHLY);
            }

            Map<Integer, List<ForumTopic>> topics = forumTopicDao.findByNotificationFrequencies(frequencies);

            for (Map.Entry<Integer, List<ForumTopic>> entry : topics.entrySet()) {
                User user = userDao.findById(entry.getKey());

                sendMailWithNewTopics(user, entry.getValue());
            }
        } catch (Throwable e) {
            log.error("Error while creating forum notifications", e);
        }
    }

    private void sendMailWithNewTopics(User user, List<ForumTopic> topics) throws MessagingException {
        List<ForumTopic> userTopics = topics.stream().filter(topic -> topic.getUserId() == user.getId()).collect(Collectors.toList());
        List<ForumTopic> otherTopics = topics.stream().filter(topic -> topic.getUserId() != user.getId()).collect(Collectors.toList());

        // load translations for users preferred language
        ResourceBundle msg = LanguageBundle.getLanguageBundle(user.getLocale());
        String serverUrl = Learnweb.config().getServerUrl();

        Mail mail = new Mail();
        mail.setSubject(msg.getString("email_forum_notifications.email_subject"));
        mail.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));

        StringBuilder content = new StringBuilder();

        content.append("<html><head><style type=\"text/css\">" +
            "  table{" +
            "    border-collapse: collapse;" +
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

        if (!userTopics.isEmpty()) {
            content.append("<br>");
            content.append("<h4>").append(msg.getString("email_forum_notifications.new_answers_to_your_posts")).append("</h4>");
            content.append("<table>");
            content.append("  <tr>" +
                "    <th>" + msg.getString("group") + "</th>" +
                "    <th>" + msg.getString("title") + "</th>" +
                "    <th>" + msg.getString("last_activities") + "</th>" +
                "  </tr>");
            for (ForumTopic topic : userTopics) {
                content.append("<tr>");

                content.append("<td class=\"first-child\">");
                content.append("<a href='" + serverUrl + "/lw/group/overview.jsf?group_id=").append(topic.getGroupId())
                    .append("'>").append(topic.getGroup().getTitle()).append("</a>");
                content.append("</td>");

                content.append("<td class=\"second-child\">");
                content.append("<a href='" + serverUrl + "/lw/group/forum_topic.jsf?topic_id=").append(topic.getId())
                    .append("'>").append(topic.getTitle()).append("</a>");
                content.append("</td>");

                content.append("<td class=\"third-child\">");
                content.append(MessageFormat.format("{0, date, medium} {1, time, short}", topic.getLastPostDate(), topic.getLastPostDate()));
                content.append("</td>");

                content.append("</tr>");
            }
            content.append("</table>");
        }
        if (!otherTopics.isEmpty()) {
            content.append("<br>");
            content.append("<h4>").append(msg.getString("email_forum_notifications.other_new_posts")).append("</h4>");
            content.append("<table>");
            content.append("  <tr>" +
                "    <th>" + msg.getString("group") + "</th>" +
                "    <th>" + msg.getString("title") + "</th>" +
                "    <th>" + msg.getString("last_activities") + "</th>" +
                "  </tr>");
            for (ForumTopic topic : otherTopics) {
                content.append("<tr>");

                content.append("<td class=\"first-child\">");
                content.append("<a href='" + serverUrl + "/lw/group/overview.jsf?group_id=").append(topic.getGroupId())
                    .append("'>").append(topic.getGroup().getTitle()).append("</a>");
                content.append("</td>");

                content.append("<td class=\"second-child\">");
                content.append("<a href='" + serverUrl + "/lw/group/forum_topic.jsf?topic_id=").append(topic.getId())
                    .append("'>").append(topic.getTitle()).append("</a>");
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
        content.append("<a href='" + serverUrl + "/lw/myhome/profile.jsf?user_id=").append(user.getId()).append("'>")
            .append(msg.getString("email_forum_notifications.change_how_often_we_send_mails")).append("</a>.");
        content.append("</li>");
        content.append("<li>");
        content.append("<a href='" + serverUrl + "/lw/user/unsubscribe.jsf?hash=").append(getHash(user)).append("'>")
            .append(msg.getString("notification_settings.unsubscribe_from_all")).append("</a>.");
        content.append("</li>");
        content.append("</ul>");
        content.append("</body></html>");

        mail.setHTML(content.toString());
        mail.sendMail();
    }

    public static String getHash(User user) {
        return user.getId() + ":" + HashHelper.sha512(Learnweb.SALT_1 + user.getId() + Learnweb.SALT_2 + "notification");
    }
}
