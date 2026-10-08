package de.l3s.learnweb.logging;

import de.l3s.learnweb.forum.ForumPost;
import de.l3s.learnweb.forum.ForumTopic;

/**
 * An event about a forum topic or post, the topic title is logged as params.
 */
public class ForumEvent extends ActivityEvent {
    public ForumEvent(Action action, ForumTopic topic) {
        this(action, topic, topic.getId());
    }

    public ForumEvent(Action action, ForumTopic topic, ForumPost post) {
        this(action, topic, post.getId());
    }

    private ForumEvent(Action action, ForumTopic topic, int targetId) {
        super(action, topic.getGroupId(), targetId, topic.getTitle());
    }
}
