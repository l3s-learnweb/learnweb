package de.l3s.learnweb.dashboard.tracker;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.Duration;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

public class TrackerUserActivity implements Serializable {
    private static final long serialVersionUID = 751932020577605715L;

    private int userId;
    private int totalEvents;
    private int timeStay;
    private int timeActive;
    private int clicks;
    private int keyPresses;

    private long timeActiveInMinutes;
    private String timeActiveFormatted;
    private long timeStayInMinutes;
    private String timeStayFormatted;

    private transient User user;

    public User getUser() throws SQLException {
        if (null == user && userId > 0) {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
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

    public int getTimeStay() {
        return timeStay;
    }

    public void setTimeStay(int timeStay) {
        this.timeStay = timeStay;

        Duration durationStay = Duration.ofMillis(timeStay);
        this.timeStayInMinutes = durationStay.toMinutes();
        this.timeStayFormatted = StringHelper.formatDuration(durationStay);
    }

    public int getTimeActive() {
        return timeActive;
    }

    public void setTimeActive(int timeActive) {
        this.timeActive = timeActive;

        Duration durationActive = Duration.ofMillis(timeActive);
        this.timeActiveInMinutes = durationActive.toMinutes();
        this.timeActiveFormatted = StringHelper.formatDuration(durationActive);
    }

    public int getClicks() {
        return clicks;
    }

    public void setClicks(int clicks) {
        this.clicks = clicks;
    }

    public int getKeyPresses() {
        return keyPresses;
    }

    public void setKeyPresses(int keyPresses) {
        this.keyPresses = keyPresses;
    }

    public long getTimeActiveInMinutes() {
        return timeActiveInMinutes;
    }

    public void setTimeActiveInMinutes(long timeActiveInMinutes) {
        this.timeActiveInMinutes = timeActiveInMinutes;
    }

    public String getTimeActiveFormatted() {
        return timeActiveFormatted;
    }

    public void setTimeActiveFormatted(String timeActiveFormatted) {
        this.timeActiveFormatted = timeActiveFormatted;
    }

    public long getTimeStayInMinutes() {
        return timeStayInMinutes;
    }

    public void setTimeStayInMinutes(long timeStayInMinutes) {
        this.timeStayInMinutes = timeStayInMinutes;
    }

    public String getTimeStayFormatted() {
        return timeStayFormatted;
    }

    public void setTimeStayFormatted(String timeStayFormatted) {
        this.timeStayFormatted = timeStayFormatted;
    }
}
