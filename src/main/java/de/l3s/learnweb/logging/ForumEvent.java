package de.l3s.learnweb.logging;

import de.l3s.learnweb.forum.ForumPost;
import de.l3s.learnweb.forum.ForumTopic;

/**
 * An event about a forum topic or post, the topic title is logged as params.
 */
public class ForumEvent extends ActivityEvent {
    public ForumEvent(Action action, ForumTopic topic) {
        super(action);
        setGroupId(topic.getGroupId());
        setTopicId(topic.getId());
        setParams(topic.getTitle());
    }

    public ForumEvent(Action action, ForumTopic topic, ForumPost post) {
        this(action, topic);
        setPostId(post.getId());
    }
}
