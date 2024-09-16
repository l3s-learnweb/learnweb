package de.l3s.learnweb.forum;

import java.io.Serial;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.User.NotificationFrequency;
import de.l3s.learnweb.user.UserDao;
import de.l3s.mail.Mail;
import de.l3s.mail.MailFactory;
import de.l3s.mail.MailService;
import de.l3s.util.HashHelper;

public class ForumNotificator implements Runnable, Serializable {
    @Serial
    private static final long serialVersionUID = -7141107765791779330L;
    private static final Logger log = LogManager.getLogger(ForumNotificator.class);

    @Inject
    private UserDao userDao;

    @Inject
    private ForumTopicDao forumTopicDao;

    @Inject
    private MailService mailService;

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
                User user = userDao.findByIdOrElseThrow(entry.getKey());

                sendMailWithNewTopics(user, entry.getValue());
            }
        } catch (Throwable e) {
            log.error("Error while creating forum notifications", e);
        }
    }

    private void sendMailWithNewTopics(User user, List<ForumTopic> topics) throws MessagingException {
        List<ForumTopic> userTopics = topics.stream().filter(topic -> topic.getUserId() == user.getId()).toList();
        List<ForumTopic> otherTopics = topics.stream().filter(topic -> topic.getUserId() != user.getId()).toList();

        Mail mail = MailFactory.buildForumNotificationEmail(user.getUsername(), userTopics, otherTopics, getHash(user)).build(user.getLocale());
        mail.addRecipient(user.getEmail());
        mailService.send(mail);
    }

    public static String getHash(User user) {
        return user.getId() + ":" + HashHelper.sha512(Learnweb.SALT_1 + user.getId() + Learnweb.SALT_2 + "notification");
    }
}
