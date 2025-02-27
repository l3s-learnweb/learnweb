package de.l3s.learnweb.logging;

import java.io.Serial;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.user.User;

@ApplicationScoped
public class LoggingEventListener implements LearnwebEventListener {
    @Serial
    private static final long serialVersionUID = -2665514411435623504L;
    private static final Logger log = LogManager.getLogger(LoggingEventListener.class);

    @Inject
    private LogDao logDao;

    @Override
    public void onEvent(final LearnwebEvent event, final User user, final String sessionId) {
        User performingUser = user;
        int groupId = 0;
        int targetId = 0;

        if (event.getTargetUser() != null) {
            if (performingUser == null) {
                performingUser = event.getTargetUser();
            }

            if (event.getAction().getTargetId() == ActionTargetId.USER_ID) {
                targetId = event.getTargetUser().getId();
            }
        }

        if (event.getTargetId() != null && event.getAction().getTargetId() != null) {
            targetId = event.getTargetId();
        }

        if (event instanceof LearnwebGroupEvent groupEvent) {
            groupId = groupEvent.getGroup().getId();

            if (event.getAction().getTargetId() == ActionTargetId.GROUP_ID) {
                targetId = groupEvent.getGroup().getId();
            }
        }

        if (event instanceof LearnwebResourceEvent resourceEvent) {
            if (event.getAction().getTargetId() == ActionTargetId.RESOURCE_ID) {
                targetId = resourceEvent.getResource().getId();
            }
        }

        logDao.insert(performingUser, event.getAction(), groupId, targetId, event.getParams(), sessionId);
    }
}
