package de.l3s.office.history.model;

import java.util.List;

public class SavingInfo
{
    private String lastsave;

    private Boolean notmodified;

    private String changesurl;

    private History history;

    private List<Action> actions;

    private String key;

    private String url;

    private List<Integer> users;

    private int status;

    public String getLastSave()
    {
        return lastsave;
    }

    public void setLastSave(String lastSave)
    {
        this.lastsave = lastSave;
    }

    public Boolean getNotmodified()
    {
        return notmodified;
    }

    public void setNotmodified(Boolean notmodified)
    {
        this.notmodified = notmodified;
    }

    public String getChangesurl()
    {
        return changesurl;
    }

    public void setChangesurl(String changesurl)
    {
        this.changesurl = changesurl;
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
