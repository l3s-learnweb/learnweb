package de.l3s.learnweb.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.enterprise.event.Event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;

@ExtendWith(MockitoExtension.class)
class EventDispatcherTest {
    @Mock
    private UserBean userBean;
    @Mock
    private Event<ActivityEvent> events;
    @InjectMocks
    private EventDispatcher eventDispatcher;

    @Test
    void fireUsesCurrentUserAndSession() {
        User user = new User();
        when(userBean.getUser()).thenReturn(user);
        when(userBean.getSessionId()).thenReturn("A1B2C3");

        ActivityEvent event = new ActivityEvent(Action.logout);
        eventDispatcher.fire(event);

        verify(events).fireAsync(event);
        assertSame(user, event.getPerformer());
        assertEquals("A1B2C3", event.getSessionId());
    }

    @Test
    void fireWithPerformerUsesCurrentSession() {
        User user = new User();
        when(userBean.getSessionId()).thenReturn("A1B2C3");

        ActivityEvent event = new ActivityEvent(Action.register);
        eventDispatcher.fire(event, user);

        verify(events).fireAsync(event);
        verify(userBean, never()).getUser();
        assertSame(user, event.getPerformer());
        assertEquals("A1B2C3", event.getSessionId());
    }

    @Test
    void fireOutsideSessionDoesNotTouchUserBean() {
        User user = new User();

        ActivityEvent event = new ActivityEvent(Action.changing_office_resource);
        eventDispatcher.fire(event, user, "onlyoffice");

        verify(events).fireAsync(event);
        verifyNoInteractions(userBean);
        assertSame(user, event.getPerformer());
        assertEquals("onlyoffice", event.getSessionId());
    }
}
