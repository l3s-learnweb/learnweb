package de.l3s.mail.message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.forum.ForumTopic;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.learnweb.web.Request;
import de.l3s.mail.MailFactory;
import de.l3s.test.LearnwebExtension;

class MessageBuilderTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();

    private static final MessagesBundle msg = new MessagesBundle(Locale.ENGLISH);
    private static final java.util.List<Request> suspiciousRequests;
    private static final java.util.List<ForumTopic> userTopics;
    private static final java.util.List<ForumTopic> otherTopics;

    static {
        Request req = mock(Request.class);
        when(req.getAddr()).thenReturn("127.0.0.1");
        when(req.getUrl()).thenReturn("https://learnweb/");
        when(req.getCreatedAt()).thenReturn(LocalDateTime.of(2021, 1, 1, 0, 0, 0));

        suspiciousRequests = java.util.List.of(req);

        Group group = mock(Group.class);
        when(group.getId()).thenReturn(123);
        when(group.getTitle()).thenReturn("GroupTitle");

        ForumTopic forumTopicMock = mock(ForumTopic.class);
        when(forumTopicMock.getId()).thenReturn(1);
        when(forumTopicMock.getTitle()).thenReturn("TopicTitle");
        when(forumTopicMock.getGroupId()).thenReturn(123);
        when(forumTopicMock.getGroup()).thenReturn(group);
        when(forumTopicMock.getUpdatedAt()).thenReturn(LocalDateTime.of(2021, 3, 1, 0, 0, 0));
        otherTopics = java.util.List.of(forumTopicMock);
        userTopics = java.util.List.of(forumTopicMock);
    }

    @Test
    void testConfirmationEmail() {
        MessageBuilder builder = MailFactory.buildConfirmEmail("testuser1", "https://learnweb/hash");
        assertEquals("Confirmation request from Learnweb", builder.getSubject(msg));
        assertEquals("""
            Hello testuser1,

            please use this link to confirm your mail address:
            https://learnweb/hash

            You can just ignore this email, if you haven't requested it.

            _____________________________________
            Best regards,
            Learnweb Team""", builder.buildPlainText(msg));
        assertEquals("<html><head><meta charset=\"UTF-8\"></head><body><p>Hello testuser1,</p><p>please use this link to confirm your mail address:"
            + "<br/><a href=\"https://learnweb/hash\">https://learnweb/hash</a></p><p>You can just ignore this email, if you haven't requested it.</p>"
            + "<footer><hr/>Best regards,<br/>Learnweb Team</footer></body></html>", builder.buildHtmlText(msg));
    }

    @Test
    void testConfirmPasswordEmail() {
        MessageBuilder builder = MailFactory.buildPasswordChangeEmail("testuser1", "test@example.com", "https://learnweb/change");
        assertEquals("Reset your Learnweb password", builder.getSubject(msg));
        assertEquals("""
            Hello testuser1,

            We have received a request to reset the password for the Learnweb account associated with test@example.com.
            You can reset your password by clicking the link below:
            https://learnweb/change

            The link will expire in 24 hours.
            If you did not request a new password, you can ignore this email, or let us know by replying to this email.

            _____________________________________
            Best regards,
            Learnweb Team""", builder.buildPlainText(msg));
        assertEquals("<html><head><meta charset=\"UTF-8\"></head><body><p>Hello testuser1,</p><p>We have received a request to reset the password"
            + " for the Learnweb account associated with test@example.com.<br/>You can reset your password by clicking the link below:<br/>"
            + "<a href=\"https://learnweb/change\">https://learnweb/change</a></p><p>The link will expire in 24 hours.<br/>"
            + "If you did not request a new password, you can ignore this email, or let us know by replying to this email.</p>"
            + "<footer><hr/>Best regards,<br/>Learnweb Team</footer></body></html>", builder.buildHtmlText(msg));
    }

    @Test
    void testRequestManager() {
        MessageBuilder builder = MailFactory.buildSuspiciousAlertEmail(suspiciousRequests);
        assertEquals("[Learnweb] Suspicious activity alert", builder.getSubject(msg));
        assertEquals("""
            Multiple accounts have been flagged as suspicious by Learnweb protection system. Please look at them closer at
            https://learnweb.l3s.uni-hannover.de/lw/admin/banlist.jsf

            Here are the ten most recent entries in the suspicious list:
            127.0.0.1\t0\t2021-01-01T00:00


            """, builder.buildPlainText(msg));
        assertEquals("<html><head><meta charset=\"UTF-8\"></head><body><p>Multiple accounts have been flagged as suspicious by Learnweb protection"
            + " system. Please look at them closer at<br/><a href=\"https://learnweb.l3s.uni-hannover.de/lw/admin/banlist.jsf\">"
            + "https://learnweb.l3s.uni-hannover.de/lw/admin/banlist.jsf</a></p><p>Here are the ten most recent entries in the suspicious list:"
            + "<table><tr><th>127.0.0.1</th><th>0</th><th>2021-01-01T00:00</th></tr></table></p></body></html>", builder.buildHtmlText(msg));
    }

    @Test
    void testContactEmail() {
        MessageBuilder builder = MailFactory.buildContactFormEmail("testuser", "testmail@gmx.de", "ABC");
        assertEquals("Contact form message", builder.getSubject(msg));
        assertEquals("""
            Name: testuser
            Email: testmail@gmx.de
            Message: ABC

            """, builder.buildPlainText(msg));
        assertEquals("<html><head><meta charset=\"UTF-8\"></head><body><p>Name: testuser<br/>Email: testmail@gmx.de<br/>Message: ABC</p>"
            + "</body></html>", builder.buildHtmlText(msg));
    }

    @Test
    void testForumNotificationEmail() {
        MessageBuilder builder = MailFactory.buildForumNotificationEmail("testuser", userTopics, otherTopics, "12");
        assertEquals("Forum notifications", builder.getSubject(msg));
        assertEquals("""
            Hello testuser,
            recent updates on your groups forums:

            New answers to your posts

            Group\tTitle\tLast activities
            GroupTitle\tTopicTitle (https://learnweb.l3s.uni-hannover.de/lw/group/forum_topic.jsf?topic_id=1)\tMarch 1, 2021, 12:00:00 AM UTC

            Other new posts

            Group\tTitle\tLast activities
            GroupTitle\tTopicTitle (https://learnweb.l3s.uni-hannover.de/lw/group/forum_topic.jsf?topic_id=1)\tMarch 1, 2021, 12:00:00 AM UTC


            * You can change how often we send you emails (https://learnweb.l3s.uni-hannover.de/lw/myhome/profile.jsf)
            * Unsubscribe from all summary emails (https://learnweb.l3s.uni-hannover.de/lw/user/unsubscribe.jsf?hash=12)

            _____________________________________
            Best regards,
            Learnweb Team""", builder.buildPlainText(msg));
        assertEquals("<html><head><meta charset=\"UTF-8\"><style type=\"text/css\">table{    border-collapse: collapse;    width:100%;  }  th{    "
            + "border: 1px solid #dddddd;    text-align: left;    padding: 8px;  }  td{    border: 1px solid #dddddd;    text-align: left;    padding: 8px;  }"
            + "  .first-child{    width:20%;    max-width: 0;    white-space: nowrap;    overflow: hidden;    text-overflow: ellipsis;  }  .second-child{    "
            + "max-width: 0;    white-space: nowrap;    overflow: hidden;    text-overflow: ellipsis;    word-wrap:break-word;  }  .third-child{    width:15%;"
            + "    word-wrap:break-word;  }  ul{    padding-left:0;  }  h4{    margin-bottom:0;  }</style></head><body><p>Hello testuser,<br/>recent updates "
            + "on your groups forums:</p><h4>New answers to your posts</h4><table><tr><th>Group</th><th>Title</th><th>Last activities</th></tr><tr>"
            + "<td class = \"first-child\">GroupTitle</td><td class = \"second-child\">"
            + "<a href=\"https://learnweb.l3s.uni-hannover.de/lw/group/forum_topic.jsf?topic_id=1\">TopicTitle</a></td><td class = \"third-child\">"
            + "March 1, 2021, 12:00:00 AM UTC</td></tr></table><br/><h4>Other new posts</h4><table><tr><th>Group</th><th>Title</th><th>Last activities</th>"
            + "</tr><tr><td class = \"first-child\">GroupTitle</td><td class = \"second-child\">"
            + "<a href=\"https://learnweb.l3s.uni-hannover.de/lw/group/forum_topic.jsf?topic_id=1\">TopicTitle</a></td><td class = \"third-child\">"
            + "March 1, 2021, 12:00:00 AM UTC</td></tr></table><br/><ul><li><a href=\"https://learnweb.l3s.uni-hannover.de/lw/myhome/profile.jsf\">"
            + "You can change how often we send you emails</a></li><li><a href=\"https://learnweb.l3s.uni-hannover.de/lw/user/unsubscribe.jsf?hash=12\">"
            + "Unsubscribe from all summary emails</a></li></ul><footer><hr/>Best regards,<br/>Learnweb Team</footer></body></html>", builder.buildHtmlText(msg));
    }

    @Disabled("Render html file with mails")
    @Test
    void testRenderHtml() throws IOException {
        File htmlFile = new File("rendered-emails.html");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFile, StandardCharsets.UTF_8))) {
            bw.write(MailFactory.buildConfirmEmail("testuser1", "https://learnweb/hash").buildHtmlText(msg));
            bw.write("<br/><hr/><br/>");
            bw.write(MailFactory.buildPasswordChangeEmail("testuser1", "test@example.com", "https://learnweb/change").buildHtmlText(msg));
            bw.write("<br/><hr/><br/>");
            bw.write(MailFactory.buildSuspiciousAlertEmail(suspiciousRequests).buildHtmlText(msg));
            bw.write("<br/><hr/><br/>");
            bw.write(MailFactory.buildContactFormEmail("testuser", "testmail@gmx.de", "ABC").buildHtmlText(msg));
            bw.write("<br/><hr/><br/>");
            bw.write(MailFactory.buildForumNotificationEmail("testuser", userTopics, otherTopics, "12").buildHtmlText(msg));
        }

        Desktop.getDesktop().browse(htmlFile.toURI());
    }
}
