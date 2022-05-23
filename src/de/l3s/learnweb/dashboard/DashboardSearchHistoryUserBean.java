package de.l3s.learnweb.dashboard;

import static de.l3s.learnweb.logging.Action.adding_resource;
import static de.l3s.learnweb.logging.Action.adding_resource_metadata;
import static de.l3s.learnweb.logging.Action.adding_yourown_metadata;
import static de.l3s.learnweb.logging.Action.edit_resource;
import static de.l3s.learnweb.logging.Action.edit_resource_metadata;
import static de.l3s.learnweb.logging.Action.extended_metadata_open_dialog;
import static de.l3s.learnweb.logging.Action.group_category_search;
import static de.l3s.learnweb.logging.Action.group_metadata_search;
import static de.l3s.learnweb.logging.Action.group_resource_search;
import static de.l3s.learnweb.logging.Action.opening_resource;
import static de.l3s.learnweb.logging.Action.searching;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationDebuggingBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class DashboardSearchHistoryUserBean extends ApplicationDebuggingBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073418345L;
    private static final Logger log = Logger.getLogger(DashboardSearchHistoryUserBean.class);

    private static final String PREFERENCE_STARTDATE = "dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "dashboard_enddate";
    private static final String TRACKER_CLIENT_ID = "1";

    private Integer paramUserId = null;

    private Date startDate = null;
    private Date endDate = null;
    private User selectedUser = null;
    private List<Integer> selectedUsersIds;

    public DashboardSearchHistoryUserBean()
    {
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException
    {
        DashboardSearchHistoryUserBean dashboard = new DashboardSearchHistoryUserBean();
        dashboard.paramUserId = 11618;
        dashboard.user = Learnweb.createInstance(null).getUserManager().getUser(11618);
        dashboard.onLoad();
    }

    public void onLoad()
    {
        log.debug("onLoad");
        User user = getUser(); // the current user
        if(user == null || (!user.isModerator() && paramUserId != user.getId())) // not logged in or no privileges
            return;

        try
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);

            String savedStartDate = getPreference(PREFERENCE_STARTDATE, Long.toString(cal.getTimeInMillis())); // month ago
            String savedEndDate = getPreference(PREFERENCE_ENDDATE, Long.toString(new Date().getTime()));
            startDate = new Date(Long.parseLong(savedStartDate));
            endDate = new Date(Long.parseLong(savedEndDate));

            if(paramUserId != null)
            {
                selectedUser = getLearnweb().getUserManager().getUser(paramUserId);
                selectedUsersIds = Collections.singletonList(paramUserId);
            }
            else
            {
                selectedUser = user;
                selectedUsersIds = Collections.singletonList(user.getId());
            }

            Action[] filter = new Action[] { opening_resource, searching, adding_resource, edit_resource, group_resource_search, extended_metadata_open_dialog, adding_yourown_metadata, adding_resource_metadata, edit_resource_metadata, group_metadata_search,
                    group_category_search };
            List<LogEntry> logEntries = getLearnweb().getLogManager().getLogsByUser(paramUserId, filter, 5000);

            for(LogEntry logEntry : logEntries)
            {
                log.debug(logEntry.getDescription());
            }

        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

}