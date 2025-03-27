package de.l3s.learnweb.logging;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;

@ApplicationScoped
public class EventBus implements Serializable {
    @Serial
    private static final long serialVersionUID = -3526985807489332574L;
    private static final Logger log = LogManager.getLogger(EventBus.class);

    private final List<LearnwebEventListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    private UserBean userBean;

    @Inject
    public EventBus(Iterable<LearnwebEventListener> listeners) {
        log.debug("Event bus created");

        listeners.forEach(this::register);
        log.info("Loaded {} event listeners", this.listeners.size());
    }

    public void register(LearnwebEventListener listener) {
        log.debug("New event listener registered: {}", listener);
        listeners.add(listener);
    }

    public void unregister(LearnwebEventListener listener) {
        log.debug("Event listener unregistered: {}", listener);
        listeners.remove(listener);
    }

    public void dispatch(LearnwebEvent event) {
        log.debug("Event dispatched: {}", event);
        final User user = userBean.getUser();
        final String sessionId = userBean.getSessionId();

        CompletableFuture.runAsync(() -> {
            for (LearnwebEventListener listener : listeners) {
                if (listener.isInterestedIn(event)) {
                    try {
                        listener.onEvent(event, user, sessionId);
                    } catch (Exception e) {
                        log.error("Error processing event in listener", e);
                    }
                }
            }
        });
    }
}
