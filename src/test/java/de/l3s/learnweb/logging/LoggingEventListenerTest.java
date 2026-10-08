package de.l3s.learnweb.logging;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.User;

@ExtendWith(MockitoExtension.class)
class LoggingEventListenerTest {
    @Mock
    private LogDao logDao;
    @InjectMocks
    private LoggingEventListener listener;

    private static User user(boolean loggingEnabled) {
        Organisation organisation = new Organisation(1);
        organisation.setOption(Option.Privacy_Logging_disabled, !loggingEnabled);

        User user = new User();
        user.setOrganisation(organisation);
        return user;
    }

    @Test
    void logsEventOfUser() {
        User user = user(true);
        ActivityEvent event = new ActivityEvent(Action.logout).setParams("bye");
        event.setContext(user, "A1B2C3");

        listener.onEvent(event);

        verify(logDao).insert(user, Action.logout, event.getGroupId(), event.getTargetId(), "bye", "A1B2C3");
    }

    @Test
    void doesNotLogIfOrganisationDisabledLogging() {
        ActivityEvent event = new ActivityEvent(Action.logout);
        event.setContext(user(false), "A1B2C3");

        listener.onEvent(event);

        verifyNoInteractions(logDao);
    }

    @Test
    void doesNotLogAnonymousUser() {
        ActivityEvent event = new ActivityEvent(Action.searching);
        event.setContext(null, "A1B2C3");

        listener.onEvent(event);

        verifyNoInteractions(logDao);
    }
}
