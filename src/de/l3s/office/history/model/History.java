package de.l3s.office.history.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.office.HistoryManager;

public class History
{
    private List<Change> changes;

    private Integer id;

    private Integer resourceId;

    private Integer previousVersionFileId;

    private String created;

    private String key;

    private String serverVersion;

    private OfficeUser user;

    private Integer version;

    private Integer changesFileId;

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(Integer resourceId)
    {
        this.resourceId = resourceId;
    }

    public Integer getPreviousVersionFileId()
    {
        return previousVersionFileId;
    }

    public void setPreviousVersionFileId(Integer previousVersionFileId)
    {
        this.previousVersionFileId = previousVersionFileId;
    }

    public Integer getChangesFileId()
    {
        return changesFileId;
    }

    public void setChangesFileId(Integer changesFileId)
    {
        this.changesFileId = changesFileId;
    }

    public String getCreated()
    {
        return created;
    }

    public void setCreated(String lastSaveDate)
    {
        this.created = lastSaveDate;
    }

    public String getServerVersion()
    {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion)
    {
        this.serverVersion = serverVersion;
    }

    public List<Change> getChanges()
    {
        return changes;
    }

    public void setChanges(List<Change> changes)
    {
        this.changes = changes;
    }

    public void addChange(Change fileChange) throws SQLException
    {
        if(changes == null)
            changes = new ArrayList<>();
        HistoryManager hm = Learnweb.getInstance().getHistoryManager();
        if(id > 0)
        {
            hm.addChangeToHistory(this, fileChange);
            changes.add(fileChange);
        }
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public OfficeUser getUser()
    {
        return user;
    }

    public void setUser(OfficeUser user)
    {
        this.user = user;
    }

}
