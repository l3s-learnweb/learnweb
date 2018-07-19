package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.jena.ext.com.google.common.collect.Lists;

import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.RightPaneBean;
import de.l3s.learnweb.resource.RightPaneBean.RightPaneAction;
import de.l3s.learnweb.user.User;

@ManagedBean
@ViewScoped
public class GroupOverviewBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6297485484480890425L;

    @ManagedProperty(value = "#{rightPaneBean}")
    private RightPaneBean rightPaneBean;

    private int groupId;
    private Group group;

    private SummaryOverview groupSummary;
    private Resource clickedResource;
    private boolean allLogs = false;
    private List<LogEntry> logMessages;

    private String summaryTitle;

    public void onLoad() throws SQLException
    {
        User user = getUser();
        if(null == user) // not logged in
            return;

        group = getLearnweb().getGroupManager().getGroupById(groupId);

        if(null == group)
            addInvalidParameterMessage("group_id");
    }

    public List<LogEntry> getLogMessages()
    {
        if(null == logMessages)
        {
            loadLogs(25);
        }
        return logMessages;
    }

    public void fetchAllLogs()
    {
        allLogs = true;
        loadLogs(-1);
    }

    /**
     *
     * @param limit if limit is -1 all log entries are returned
     */
    private void loadLogs(int limit)
    {
        try
        {
            logMessages = getLearnweb().getLogsByGroup(groupId, null, limit);

        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public boolean isAllLogs()
    {
        return allLogs;
    }

    public SummaryOverview getSummaryOverview()
    {
        try
        {
            final List<Action> actions = Lists.newArrayList(LogEntry.Action.forum_post_added, LogEntry.Action.deleting_resource,
                    LogEntry.Action.adding_resource, LogEntry.Action.group_joining, LogEntry.Action.group_leaving, LogEntry.Action.forum_reply_message, LogEntry.Action.changing_resource);
            if(groupSummary == null || groupSummary.isEmpty())
            {
                groupSummary = getLearnweb().getLogsByGroup(groupId, actions, LocalDateTime.now().minusWeeks(1), LocalDateTime.now());
                summaryTitle = UtilBean.getLocaleMessage("last_week_changes");
            }
            if(groupSummary == null || groupSummary.isEmpty())
            {
                groupSummary = getLearnweb().getLogsByGroup(groupId, actions, LocalDateTime.now().minusMonths(1), LocalDateTime.now());
                summaryTitle = UtilBean.getLocaleMessage("last_month_overview_changes");
            }
            if(groupSummary == null || groupSummary.isEmpty())
            {
                groupSummary = getLearnweb().getLogsByGroup(groupId, actions, LocalDateTime.now().minusMonths(6), LocalDateTime.now());
                summaryTitle = UtilBean.getLocaleMessage("last_six_month_changes");
            }
            return groupSummary;
        }
        catch(Exception e)
        {
            addFatalMessage(e);
            return null;
        }
    }

    public AbstractMap.SimpleEntry<String, Resource> getChosenResourceFromSlider() throws SQLException
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
        List<Resource> updatedResources = new LinkedList<>();
        updatedResources.addAll(groupSummary.getUpdatedResources().keySet());
        clickedResource = updatedResources.get(index);
        return updatedResources.get(index);
    }

    public Resource getClickedResource()
    {
        return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
        this.clickedResource = clickedResource;
    }

    public List<LogEntry> getUpdatedResourceActivities()
    {
        return groupSummary.getUpdatedResources().get(clickedResource);
    }

    public void displayClickedResourceFromSlider() throws SQLException
    {
        SimpleEntry<String, Resource> clickedResourceFromSlider = getChosenResourceFromSlider();
        if(clickedResourceFromSlider != null)
        {
            rightPaneBean.setPaneAction("updated".equals(clickedResourceFromSlider.getKey()) ? RightPaneAction.viewUpdatedResource : RightPaneAction.viewResource);
            rightPaneBean.setClickedAbstractResource(clickedResourceFromSlider.getValue());
        }
    }

    public void setRightPaneBean(RightPaneBean rightPaneBean)
    {
        this.rightPaneBean = rightPaneBean;
    }

    public Group getGroup()
    {
        return group;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public String getSummaryTitle()
    {
        return summaryTitle;
    }
}
