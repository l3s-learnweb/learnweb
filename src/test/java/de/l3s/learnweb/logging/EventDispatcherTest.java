package de.l3s.learnweb.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.event.Event;

import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        lenient().when(events.fireAsync(any())) // replaced by the test of a failing observer
            .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    void fireUsesCurrentUserAndSession() {
        User user = new User();
        when(userBean.getUser()).thenReturn(user);
        when(userBean.getSessionId()).thenReturn("A1B2C3");

        ActivityEvent event = new ActivityEvent(Action.logout);
        eventDispatcher.fire(event);

        verify(events).fireAsync(event);
        verify(events, never()).fire(any()); // the request never waits for the observers
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

    @Test
    void observerFailureDoesNotBreakRequest() {
        ActivityEvent event = new ActivityEvent(Action.logout);
        when(events.fireAsync(event)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("observer failed")));

        assertDoesNotThrow(() -> eventDispatcher.fire(event, new User(), "A1B2C3"));
    }
}
