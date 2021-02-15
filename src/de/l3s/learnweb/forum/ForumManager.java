package de.l3s.learnweb.forum;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User.NotificationFrequency;

public class ForumManager {
    //private static final Logger log = LogManager.getLogger(ForumManager.class);

    private final Learnweb learnweb;
    private final ForumPostDao forumPostDao;
    private final ForumTopicDao forumTopicDao;

    public ForumManager(Learnweb learnweb) throws SQLException {
        this.learnweb = learnweb;
        forumPostDao = learnweb.getJdbi().onDemand(ForumPostDao.class);
        forumTopicDao = learnweb.getJdbi().onDemand(ForumTopicDao.class);
    }

    /**
     * returns all topic of the defined group. Sorted by topic_last_post_time
     */
    public List<ForumTopic> getTopicsByGroup(int groupId) throws SQLException {
        return forumTopicDao.findAll(groupId);
    }

    /**
     * @return number of posts per users of defined group
     */
    public Map<Integer, Integer> getPostCountPerUserByGroup(int groupId) throws SQLException {
        return forumPostDao.countPerUserByGroup(groupId);
    }

    /**
     * @return null if not found
     */
    public ForumTopic getTopicById(int topicId) throws SQLException {
        return forumTopicDao.find(topicId).orElse(null);
    }

    /**
     * Sorted by date DESC.
     */
    public List<ForumPost> getPostsBy(int topicId) throws SQLException {
        return forumPostDao.findAll(topicId);
    }

    public ForumPost getPostById(int postId) throws SQLException {
        return forumPostDao.find(postId).orElse(null);
    }

    public List<ForumPost> getPostsByUser(int userId) throws SQLException {
        return forumPostDao.findAllByUserId(userId);
    }

    public int getPostCountByUser(int userId) throws SQLException {
        return forumPostDao.countByUserId(userId);
    }

    public ForumTopic save(ForumTopic topic) throws SQLException {
        forumTopicDao.insertOrUpdate(topic);
        return topic;
    }

    public ForumPost save(ForumPost post) throws SQLException {
        forumPostDao.insertOrUpdate(post);

        if (post.getId() > 0) {
            forumTopicDao.increaseReplies(post.getTopicId(), post.getId(), post.getUserId(), post.getDate());
            post.getUser().incForumPostCount();
        }

        return post;
    }

    public void deleteTopic(ForumTopic topic) throws SQLException {
        forumTopicDao.delete(topic.getId());
    }

    /**
     * increment topic view counter.
     */
    public void incViews(int topicId) throws SQLException {
        forumTopicDao.increaseViews(topicId);
    }

    public void deletePost(ForumPost post) throws SQLException {
        forumPostDao.delete(post.getId());
    }

    /**
     * @return list of topics, that users should be notified about
     */
    public Map<Integer, List<ForumTopic>> getTopicsByNotificationFrequencies(List<NotificationFrequency> notificationFrequencies) throws SQLException {
        return forumTopicDao.findByNotificationFrequencies(notificationFrequencies);
    }

    /**
     * Updates last_visit time when user open topic.
     */
    public void updatePostVisitTime(int topicId, int userId) throws SQLException {
        forumTopicDao.registerUserVisit(topicId, userId);
    }
}
