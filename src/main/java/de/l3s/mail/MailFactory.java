package de.l3s.mail;

import java.util.List;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.forum.ForumTopic;
import de.l3s.learnweb.web.Request;
import de.l3s.mail.message.DateTime;
import de.l3s.mail.message.Heading;
import de.l3s.mail.message.LineBreak;
import de.l3s.mail.message.Link;
import de.l3s.mail.message.MessageBuilder;
import de.l3s.mail.message.Paragraph;
import de.l3s.mail.message.Table;
import de.l3s.mail.message.Text;

public final class MailFactory {
    public static MessageBuilder buildForumNotificationEmail(String username, List<ForumTopic> userTopics, List<ForumTopic> otherTopics, String unsubscribeHash) {
        String serverUrl = Learnweb.config().getServerUrl();

        MessageBuilder builder = new MessageBuilder("email_forum_notifications.email_subject")
            .headStyle("table{" +
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
                "  }");

        builder.add(new Paragraph(new Text("greeting")).append(" ").append(username).append(",")
            .append(new LineBreak()).append(new Text("email_forum_notifications.resent_updates")));

        if (!userTopics.isEmpty()) {
            builder.add(new Heading(4, "email_forum_notifications.new_answers_to_your_posts"));
            builder.add(activityTable(serverUrl, userTopics));
            builder.add(new LineBreak());
        }

        if (!otherTopics.isEmpty()) {
            builder.add(new Heading(4, "email_forum_notifications.other_new_posts"));
            builder.add(activityTable(serverUrl, userTopics));
            builder.add(new LineBreak());
        }

        builder.add(new de.l3s.mail.message.List()
            .add(new Link(serverUrl + "/lw/myhome/profile.jsf", "email_forum_notifications.change_how_often_we_send_mails"))
            .add(new Link(serverUrl + "/lw/user/unsubscribe.jsf?hash=" + unsubscribeHash, "notification_settings.unsubscribe_from_all")));

        builder.defaultFooter();
        return builder;
    }

    private static Table activityTable(String serverUrl, List<ForumTopic> topics) {
        Table table = new Table();
        table.setColumnClass("first-child", "second-child", "third-child");
        table.addRow("group", "title", "last_activities");
        for (ForumTopic topic : topics) {
            table.addRow(
                new Text(topic.getGroup().getTitle()),
                new Link(serverUrl + "/lw/group/forum_topic.jsf?topic_id=" + topic.getId(), topic.getTitle()),
                new DateTime(topic.getUpdatedAt())
            );
        }
        return table;
    }

    public static MessageBuilder buildConfirmEmail(String username, String confirmUrl) {
        return new MessageBuilder("email.confirm_address_subject", Learnweb.config().getAppName())
            .add(new Paragraph(new Text("greeting")).append(" ").append(username).append(","))
            .add(new Paragraph(new Text("email.confirm_address"), new LineBreak(), new Link(confirmUrl)))
            .add(new Paragraph(new Text("email.you_can_ignore")))
            .defaultFooter();
    }

    public static MessageBuilder buildPasswordChangeEmail(String username, String changeUrl) {
        return new MessageBuilder("email_password.retrieve_title", Learnweb.config().getAppName())
            .add(new Paragraph(new Text("greeting")).append(" ").append(username).append(","))
            .add(new Paragraph(new Text("email_password.change", Learnweb.config().getAppName(), username)).append(new LineBreak()).append(new Link(changeUrl)))
            .add(new Paragraph(new Text("email_password.expired_warning")).append(new LineBreak()).append(new Text("email.you_can_ignore")))
            .defaultFooter();
    }

    public static MessageBuilder buildContactFormEmail(String name, String email, String message) {
        return new MessageBuilder("Contact form message")
            .add(new Paragraph(new Text("Name")).append(": ").append(name).append(new LineBreak())
                .append(new Text("Email")).append(": ").append(email).append(new LineBreak())
                .append(new Text("Message")).append(": ").append(message));
    }

    public static MessageBuilder buildNotificationEmail(String title, String text, String username) {
        return new MessageBuilder("Learnweb: " + title)
            .add(new Text(text))
            .footer(new Text("mail_notification_footer", username));
    }

    public static MessageBuilder buildSuspiciousAlertEmail(List<Request> suspiciousRequests) {
        String serverUrl = Learnweb.config().getServerUrl();

        MessageBuilder builder = new MessageBuilder("[Learnweb] Suspicious activity alert");
        builder.add(new Paragraph("Multiple accounts have been flagged as suspicious by Learnweb protection system. Please look at them closer at")
            .append(new LineBreak()).append(new Link(serverUrl + "/lw/admin/banlist.jsf")));

        Table table = new Table();
        for (Request ard : suspiciousRequests) {
            table.addRow(ard.getAddr(), String.valueOf(ard.getRequests()), ard.getCreatedAt().toString());
        }

        builder.add(new Paragraph("Here are the ten most recent entries in the suspicious list:").append(table));
        return builder;
    }
}
