package de.l3s.learnweb.group;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.jena.ext.com.google.common.collect.Lists;

import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.Resource;

@ManagedBean
@ViewScoped
public class GroupSummaryBean extends ApplicationBean
{
    private SummaryOverview groupSummary;

    private int groupId;

    private Resource clickedResource;

    public SummaryOverview getSummaryOverview() throws Exception
    {
        if(getUser() == null)
        {
            return null;
        }
        final List<Action> actions = Lists.newArrayList(LogEntry.Action.forum_post_added, LogEntry.Action.deleting_resource,
                LogEntry.Action.adding_resource, LogEntry.Action.group_joining, LogEntry.Action.group_leaving, LogEntry.Action.forum_reply_message, LogEntry.Action.changing_resource);
        groupSummary = getLearnweb().getLogsByGroup(getGroupId(), actions, getRightDateFrom(), LocalDateTime.now());
        groupSummary.setDescriptions();
        return groupSummary;
    }

    private LocalDateTime getRightDateFrom() throws SQLException, Exception
    {

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(Math.max(Math.min((this.getUser().getGroups().get(0).getLastVisit(this.getUser()) * 1000l),
                ZonedDateTime.now().minusWeeks(1).toInstant().toEpochMilli()), ZonedDateTime.now().minusMonths(6).toInstant().toEpochMilli())), ZoneId.systemDefault());
    }

    public AbstractMap.SimpleEntry<String, Resource> getChoosenResourceFromSlider() throws SQLException
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String index = params.get("index");
        String type = params.get("type");
        if(groupSummary != null && index != null && type != null)
        {
            return new AbstractMap.SimpleEntry<String, Resource>(type, getClickedResourceFromOverview(Integer.valueOf(index), type));
        }
        return null;
    }

    private Resource getClickedResourceFromOverview(Integer index, String type) throws SQLException
    {
        if("added".equals(type))
        {
            clickedResource = groupSummary.getAddedResources().get(index).getResource();
            return groupSummary.getAddedResources().get(index).getResource();
        }
        List<Resource> updatedResourcces = new LinkedList<>();
        updatedResourcces.addAll(groupSummary.getUpdatedResources().keySet());
        clickedResource = updatedResourcces.get(index);
        return updatedResourcces.get(index);

    }

    public SummaryOverview getGroupSummary()
    {
        return groupSummary;
    }

    public void setGroupSummary(SummaryOverview groupSummary)
    {
        this.groupSummary = groupSummary;
    }

    public Resource getClickedResource()
    {
        return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
        this.clickedResource = clickedResource;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public List<LogEntry> getUpdatedResourceActivities()
    {
        return groupSummary.getUpdatedResources().get(clickedResource);
    }

}
