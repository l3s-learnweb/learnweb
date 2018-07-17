package de.l3s.learnweb.resource.office.history.model;

public class Change
{
    private String created;

    private OfficeUser user;

    private Integer id;

    private Integer historyId;

    public String getCreated()
    {
        return created;
    }

    public void setCreated(String created)
    {
        this.created = created;
    }

    public OfficeUser getUser()
    {
        return user;
    }

    public void setUser(OfficeUser user)
    {
        this.user = user;
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getHistoryId()
    {
        return historyId;
    }

    public void setHistoryId(Integer historyId)
    {
        this.historyId = historyId;
    }

}
