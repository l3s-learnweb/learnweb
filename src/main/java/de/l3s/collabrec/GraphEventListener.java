package de.l3s.collabrec;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.logging.ActivityEvent;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

/**
 * Mirrors the users, groups and memberships to the CollabRec knowledge graph, see {@link GraphClient}.
 */
@ApplicationScoped
public class GraphEventListener {
    private static final Logger log = LogManager.getLogger(GraphEventListener.class);

    @Inject
    private GraphClient graphClient;

    @Inject
    private UserDao userDao;

    @Inject
    private GroupDao groupDao;

    /**
     * Handles only the events of users, whose organisation allows the activity log.
     */
    public void onEvent(@ObservesAsync ActivityEvent event) {
        User performer = event.getPerformer();
        if (!graphClient.isEnabled() || performer == null || !performer.getOrganisation().isLoggingEnabled()) {
            return;
        }

        try {
            UpdateBuilder update = switch (event.getAction()) {
                case register, login -> GraphClient.updateUserNode(performer);
                case changing_profile -> userDao.findById(event.getTargetId()).map(GraphClient::updateUserNode).orElse(null);
                case group_creating, group_changing_title, group_changing_description ->
                    groupDao.findById(event.getGroupId()).map(GraphClient::updateGroupNode).orElse(null);
                case group_joining -> GraphClient.associateUserGroup(performer.getId(), event.getGroupId());
                case group_leaving -> GraphClient.dissociateUserGroup(performer.getId(), event.getGroupId());
                default -> null;
            };

            if (update != null) {
                graphClient.update(update);
            }
        } catch (GraphException e) {
            log.error("Failed to update graph on: {}", event, e);
        }
    }
}
