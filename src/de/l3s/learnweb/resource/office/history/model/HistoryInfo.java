package de.l3s.learnweb.resource.office.history.model;

import java.util.List;

public class HistoryInfo
{
    private int currentVersion;
    private List<History> history;

    public List<History> getHistory()
    {
        return history;
    }

    public void setHistory(List<History> history)
    {
        this.history = history;
    }

    public int getCurrentVersion()
    {
        return currentVersion;
    }

    public void setCurrentVersion(int currentVersion)
    {
        this.currentVersion = currentVersion;
    }
}
