package de.l3s.learnweb.dashboard.tracker;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class TrackerUserActivity implements Serializable {
    @Serial
    private static final long serialVersionUID = 751932020577605715L;

    private int userId;
    private int totalEvents;
    private long timeStay;
    private long timeActive;
    private int clicks;
    private int keypresses;

    private transient User user;
    private transient String timeActiveFormatted;
    private transient String timeStayFormatted;

    public User getUser() {
        if (null == user && userId != 0) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
        }
        return user;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getTimeStay() {
        return timeStay;
    }

    public void setTimeStay(long timeStay) {
        this.timeStay = timeStay;
    }

    public long getTimeActive() {
        return timeActive;
    }

    public void setTimeActive(long timeActive) {
        this.timeActive = timeActive;
    }

    public int getClicks() {
        return clicks;
    }

    public void setClicks(int clicks) {
        this.clicks = clicks;
    }

    public int getKeypresses() {
        return keypresses;
    }

    public void setKeypresses(int keypresses) {
        this.keypresses = keypresses;
    }

    public String getTimeActiveFormatted() {
        if (timeActiveFormatted == null) {
            timeActiveFormatted = StringHelper.formatDuration(Duration.ofMillis(timeActive));
        }
        return timeActiveFormatted;
    }

    public String getTimeStayFormatted() {
        if (timeStayFormatted == null) {
            timeStayFormatted = StringHelper.formatDuration(Duration.ofMillis(timeStay));
        }
        return timeStayFormatted;
    }
}
