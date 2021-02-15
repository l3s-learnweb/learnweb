package de.l3s.learnweb.resource.office.history.model;

import java.util.List;

/**
 * Object used to send data to `docEditor.refreshHistory` method.
 */
public class HistoryInfo {
    private final int currentVersion;
    private final List<History> history;

    public HistoryInfo(final List<History> history) {
        this.history = history;

        if (history != null && !history.isEmpty()) {
            this.currentVersion = history.get(history.size() - 1).getVersion();
        } else {
            this.currentVersion = 0;
        }
    }

    public List<History> getHistory() {
        return history;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }
}
