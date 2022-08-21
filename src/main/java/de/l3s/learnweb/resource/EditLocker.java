package de.l3s.learnweb.resource;

import java.time.Instant;

import de.l3s.learnweb.user.User;

public class EditLocker {
    private static final int LOCK_SESSION_EXPIRES_IN_MS = 2 * 60 * 1000;

    private final User user;
    private final Instant editStarted;
    private Instant lastActivity;

    public EditLocker(final User user) {
        this.user = user;
        this.lastActivity = Instant.now();
        this.editStarted = Instant.now();
    }

    public User getUser() {
        return user;
    }

    public Instant getEditStarted() {
        return editStarted;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(final Instant lastActivity) {
        this.lastActivity = lastActivity;
    }

    public boolean isSessionExpired() {
        return Instant.now().minusMillis(LOCK_SESSION_EXPIRES_IN_MS).isAfter(lastActivity);
    }
}
