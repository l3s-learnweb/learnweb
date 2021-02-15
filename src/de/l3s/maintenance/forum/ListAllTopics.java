package de.l3s.maintenance.forum;

import java.util.List;

import de.l3s.learnweb.forum.ForumTopic;
import de.l3s.maintenance.MaintenanceTask;

/**
 * An example of using DAO in runnable tasks (spoiler: like in tests)
 *
 * @author Oleh Astappiev
 */
public class ListAllTopics extends MaintenanceTask {

    @Override
    protected void run(final boolean dryRun) {
        List<ForumTopic> topics = getLearnweb().getDaoProvider().getForumTopicDao().findByGroupId(1158);

        for (ForumTopic topic : topics) {
            log.debug(topic);
        }

        log.info("Total topics: {}", topics.size());
    }

    public static void main(String[] args) {
        new ListAllTopics().start(args);
    }
}
