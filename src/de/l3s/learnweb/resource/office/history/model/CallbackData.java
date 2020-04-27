package de.l3s.learnweb.resource.office.history.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CallbackData
{
    // private List<Action> actions;
    @SerializedName("changesurl")
    private String changesUrl;
    @SerializedName("forcesavetype")
    private int forceSaveType;
    private History history;
    private String key;
    private int status;
    private String url;
    @SerializedName("userdata")
    private String userData;
    private List<Integer> users;

    public String getChangesUrl()
    {
        return changesUrl;
    }

    public void setChangesUrl(final String changesUrl)
    {
        this.changesUrl = changesUrl;
    }

    public int getForceSaveType()
    {
        return forceSaveType;
    }

    public void setForceSaveType(final int forceSaveType)
    {
        this.forceSaveType = forceSaveType;
    }

    public History getHistory()
    {
        return history;
    }

    public void setHistory(final History history)
    {
        this.history = history;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(final String key)
    {
        this.key = key;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(final int status)
    {
        this.status = status;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    public String getUserData()
    {
        return userData;
    }

    public void setUserData(final String userData)
    {
        this.userData = userData;
    }

    public List<Integer> getUsers()
    {
        return users;
    }

    public void setUsers(final List<Integer> users)
    {
        this.users = users;
    }
}
