package de.l3s.learnweb.resource.office.history.model;

import java.util.List;

/**
 * Object used to send data to `docEditor.refreshHistory` method.
 */
public class HistoryInfo {
    private int currentVersion;
    private List<History> history;

    public List<History> getHistory() {
        return history;
    }

    public void setHistory(final List<History> history) {
        this.history = history;

        if (history != null && !history.isEmpty()) {
            this.currentVersion = history.get(history.size() - 1).getVersion();
        }
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(final int currentVersion) {
        this.currentVersion = currentVersion;
    }
}
