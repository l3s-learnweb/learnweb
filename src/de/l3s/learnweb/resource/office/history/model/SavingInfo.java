package de.l3s.learnweb.resource.office.history.model;

import java.util.List;

public class SavingInfo
{
    private String lastSave;

    private Boolean notModified;

    private String changesUrl;

    private History history;

    private List<Action> actions;

    private String key;

    private String url;

    private List<Integer> users;

    private int status;

    public String getLastSave()
    {
        return lastSave;
    }

    public void setLastSave(String lastSave)
    {
        this.lastSave = lastSave;
    }

    public Boolean getNotModified()
    {
        return notModified;
    }

    public void setNotModified(Boolean notModified)
    {
        this.notModified = notModified;
    }

    public String getChangesUrl()
    {
        return changesUrl;
    }

    public void setChangesUrl(String changesUrl)
    {
        this.changesUrl = changesUrl;
    }

    public History getHistory()
    {
        return history;
    }

    public void setHistory(History history)
    {
        this.history = history;
    }

    public List<Action> getActions()
    {
        return actions;
    }

    public void setActions(List<Action> actions)
    {
        this.actions = actions;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public List<Integer> getUsers()
    {
        return users;
    }

    public void setUsers(List<Integer> users)
    {
        this.users = users;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

}
