package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.RightPaneBean;
import de.l3s.learnweb.resource.RightPaneBean.RightPaneAction;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupOverviewBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6297485484480890425L;
    private static final Logger log = Logger.getLogger(GroupOverviewBean.class);

    private int groupId;
    private Group group;

    private String summaryTitle;
    private boolean showAllLogs = false;

    private List<LogEntry> logMessages;
    private SummaryOverview groupSummary;
    private List<User> members;

    private Resource clickedResource;
    private User clickedUser;

    @Inject
    private RightPaneBean rightPaneBean;

    public void onLoad() throws SQLException
    {
        User user = getUser();
        if(null == user) // not logged in
            return;

        group = getLearnweb().getGroupManager().getGroupById(groupId);

        if(null == group)
            addInvalidParameterMessage("group_id");

        if(null != group)
        {
            user.setActiveGroup(group);
            group.setLastVisit(user);
        }
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
        showAllLogs = true;
        loadLogs(-1);
    }

    /**
     * @param limit if limit is -1 all log entries are returned
     */
    private void loadLogs(int limit)
    {
        try
        {
            logMessages = getLearnweb().getLogManager().getLogsByGroup(groupId, null, limit);
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public boolean isShowAllLogs()
    {
        return showAllLogs;
    }

    private final static Action[] OVERVIEW_ACTIONS = { Action.forum_topic_added, Action.deleting_resource,
            Action.adding_resource, Action.group_joining, Action.group_leaving, Action.forum_post_added, Action.changing_office_resource };

    public SummaryOverview getSummaryOverview()
    {
        try
        {
            if(groupSummary == null || groupSummary.isEmpty())
            {
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, OVERVIEW_ACTIONS, LocalDateTime.now().minusWeeks(1), LocalDateTime.now());
                summaryTitle = UtilBean.getLocaleMessage("last_week_changes");
            }
            if(groupSummary == null || groupSummary.isEmpty())
            {
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, OVERVIEW_ACTIONS, LocalDateTime.now().minusMonths(1), LocalDateTime.now());
                summaryTitle = UtilBean.getLocaleMessage("last_month_overview_changes");
            }
            if(groupSummary == null || groupSummary.isEmpty())
            {
                groupSummary = getLearnweb().getLogManager().getLogsByGroup(groupId, OVERVIEW_ACTIONS, LocalDateTime.now().minusMonths(6), LocalDateTime.now());
                summaryTitle = UtilBean.getLocaleMessage("last_six_month_changes");
            }
            return groupSummary;
        }
        catch(Exception e)
        {
            log.error("Can't create group summery", e);
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
            if(StringUtils.isEmpty(index))
            { // TODO: remove later, added to investigate issue
                log.error("getChosenResourceFromSlider: index is empty for type `" + type + "`.");
            }

            return new AbstractMap.SimpleEntry<>(type, getClickedResourceFromOverview(Integer.valueOf(index), type));
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
            rightPaneBean.setPaneAction(RightPaneAction.viewResource);
            rightPaneBean.setClickedAbstractResource(clickedResourceFromSlider.getValue());
        }
    }

    public String getSummaryTitle()
    {
        return summaryTitle;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public Group getGroup()
    {
        return group;
    }

    public void setRightPaneBean(RightPaneBean rightPaneBean)
    {
        this.rightPaneBean = rightPaneBean;
    }

    public User getClickedUser()
    {
        return clickedUser;
    }

    public void setClickedUser(User clickedUser)
    {
        this.clickedUser = clickedUser;
    }

    public boolean isMember() throws SQLException
    {
        User user = getUser();

        if(null == user)
            return false;

        if(null == group)
            return false;

        return group.isMember(user);
    }

    public List<User> getMembers() throws SQLException
    {
        if(null == members && group != null)
        {
            members = group.getMembers();
        }
        return members;
    }

    public boolean isUserDetailsHidden()
    {
        User user = getUser();
        if(user == null)
            return false;
        if(user.getOrganisation().getId() == 1249 && user.getOrganisation().getOption(Organisation.Option.Privacy_Anonymize_usernames))
            return true;
        return false;
    }
}
