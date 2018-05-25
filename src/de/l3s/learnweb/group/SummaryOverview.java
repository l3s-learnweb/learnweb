package de.l3s.learnweb.group;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.resource.Resource;

public class SummaryOverview
{
    private final List<LogEntry> addedResources = new LinkedList<>();
    private final List<LogEntry> deletedResources = new LinkedList<>();
    private final List<LogEntry> forumsInfo = new LinkedList<>();
    private final List<LogEntry> membersInfo = new LinkedList<>();
    private final Map<Resource, List<LogEntry>> updatedResources = new HashMap<>();

    private String addedResourcesDescription;
    private String deletedResourcesDescription;
    private String updatedResourcesDescription;
    private String forumInfoDescription;
    private String membersInfoDescription;

    public String getAddedResourcesDescription()
    {
        return addedResourcesDescription;
    }

    public String getDeletedResourcesDescription()
    {
        return deletedResourcesDescription;
    }

    public String getUpdatedResourcesDescription()
    {
        return updatedResourcesDescription;
    }

    public String getForumInfoDescription()
    {
        return forumInfoDescription;
    }

    public String getMembersInfoDescription()
    {
        return membersInfoDescription;
    }

    public List<LogEntry> getAddedResources()
    {
        return addedResources;
    }

    public List<LogEntry> getDeletedResources()
    {
        return deletedResources;
    }

    public List<LogEntry> getForumsInfo()
    {
        return forumsInfo;
    }

    public List<LogEntry> getMembersInfo()
    {
        return membersInfo;
    }

    public boolean isEmpty()
    {
        return addedResources.isEmpty() && deletedResources.isEmpty() && forumsInfo.isEmpty() && membersInfo.isEmpty() && getUpdatedResources().isEmpty();
    }

    public Map<Resource, List<LogEntry>> getUpdatedResources()
    {
        return updatedResources;
    }

    public void generateDescriptions()
    {
        if(!getAddedResources().isEmpty())
        {
            addedResourcesDescription = (getAddedResources().size() > 1 ? UtilBean.getLocaleMessage("added_resources", getAddedResources().size()) : UtilBean.getLocaleMessage("added_one_resource"));
        }
        if(!getDeletedResources().isEmpty())
        {
            deletedResourcesDescription = (getDeletedResources().size() > 1 ? UtilBean.getLocaleMessage("deleted_resources", getDeletedResources().size()) : UtilBean.getLocaleMessage("deleted_one_resource"));
        }
        if(!getUpdatedResources().isEmpty())
        {
            updatedResourcesDescription = (getUpdatedResources().size() > 1 ? UtilBean.getLocaleMessage("updated_resources", getUpdatedResources().size()) : UtilBean.getLocaleMessage("updated_one_resource"));
        }
        if(!getForumsInfo().isEmpty())
        {
            forumInfoDescription = (UtilBean.getLocaleMessage("forum_activity"));
        }
        if(!getMembersInfo().isEmpty())
        {
            membersInfoDescription = (UtilBean.getLocaleMessage("new_members_info"));
        }

    }
}
