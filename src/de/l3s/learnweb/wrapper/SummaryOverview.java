package de.l3s.learnweb.wrapper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.beans.UtilBean;

public class SummaryOverview
{
    private List<LogEntry> addedResources;

    private List<LogEntry> deletedResources;

    private List<LogEntry> forumsInfo;

    private List<LogEntry> membersInfo;

    private Map<Resource, List<LogEntry>> updatedResources;

    private String addedResourcesDescription;
    private String deletedResourcesDescription;
    private String updatedResourcesDescription;
    private String forumInfoDescription;
    private String membersInfoDescription;

    public SummaryOverview()
    {
        addedResources = new LinkedList<>();
        deletedResources = new LinkedList<>();
        forumsInfo = new LinkedList<>();
        membersInfo = new LinkedList<>();
        setUpdatedResources(new HashMap<>());
    }

    public String getAddedResourcesDescription()
    {
        return addedResourcesDescription;
    }

    public void setAddedResourcesDescription(String addedResourcesDescription)
    {
        this.addedResourcesDescription = addedResourcesDescription;
    }

    public String getDeletedResourcesDescription()
    {
        return deletedResourcesDescription;
    }

    public void setDeletedResourcesDescription(String deletedResourcesDescription)
    {
        this.deletedResourcesDescription = deletedResourcesDescription;
    }

    public String getUpdatedResourcesDescription()
    {
        return updatedResourcesDescription;
    }

    public void setUpdatedResourcesDescription(String updatedResourcesDescription)
    {
        this.updatedResourcesDescription = updatedResourcesDescription;
    }

    public String getForumInfoDescription()
    {
        return forumInfoDescription;
    }

    public void setForumInfoDescription(String forumInfoDescription)
    {
        this.forumInfoDescription = forumInfoDescription;
    }

    public String getMembersInfoDescription()
    {
        return membersInfoDescription;
    }

    public void setMembersInfoDescription(String membersInfoDescription)
    {
        this.membersInfoDescription = membersInfoDescription;
    }

    public List<LogEntry> getAddedResources()
    {
        return addedResources;
    }

    public void setAddedResources(List<LogEntry> addedResources)
    {
        this.addedResources = addedResources;
    }

    public List<LogEntry> getDeletedResources()
    {
        return deletedResources;
    }

    public void setDeletedResources(List<LogEntry> deletedResources)
    {
        this.deletedResources = deletedResources;
    }

    public List<LogEntry> getForumsInfo()
    {
        return forumsInfo;
    }

    public void setForumsInfo(List<LogEntry> createdForums)
    {
        this.forumsInfo = createdForums;
    }

    public List<LogEntry> getMembersInfo()
    {
        return membersInfo;
    }

    public void setMembersInfo(List<LogEntry> membersInfo)
    {
        this.membersInfo = membersInfo;
    }

    public boolean isEmpty()
    {
        return addedResources.isEmpty() && deletedResources.isEmpty() && forumsInfo.isEmpty() && membersInfo.isEmpty() && getUpdatedResources().isEmpty();
    }

    public Map<Resource, List<LogEntry>> getUpdatedResources()
    {
        return updatedResources;
    }

    public void setUpdatedResources(Map<Resource, List<LogEntry>> updatedResources)
    {
        this.updatedResources = updatedResources;
    }

    public void setDescriptions()
    {
        if(!getAddedResources().isEmpty())
        {
            setAddedResourcesDescription(getAddedResources().size() > 1 ? UtilBean.getLocaleMessage("added_resources", getAddedResources().size()) : UtilBean.getLocaleMessage("added_one_resource"));
        }
        if(!getDeletedResources().isEmpty())
        {
            setDeletedResourcesDescription(getDeletedResources().size() > 1 ? UtilBean.getLocaleMessage("deleted_resources", getDeletedResources().size()) : UtilBean.getLocaleMessage("deleted_one_resource"));
        }
        if(!getUpdatedResources().isEmpty())
        {
            setUpdatedResourcesDescription(getUpdatedResources().size() > 1 ? UtilBean.getLocaleMessage("updated_resources", getUpdatedResources().size()) : UtilBean.getLocaleMessage("updated_one_resource"));
        }
        if(!getForumsInfo().isEmpty())
        {
            setForumInfoDescription(UtilBean.getLocaleMessage("forum_activity"));
        }
        if(!getMembersInfo().isEmpty())
        {
            setMembersInfoDescription(UtilBean.getLocaleMessage("new_members_info"));
        }

    }

}
