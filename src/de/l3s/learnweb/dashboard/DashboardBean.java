package de.l3s.learnweb.dashboard;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryFieldSummery;

@ManagedBean()
@ViewScoped
public class DashboardBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073418345L;
    private static final Logger log = Logger.getLogger(DashboardBean.class);
    private static final String PREFERENCE_STARTDATE = "dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "dashboard_enddate";

    private Course selectedCourse; // the visualized course
    private Date startDate;
    private Date endDate;
    private List<GlossaryFieldSummery> glossaryFieldSummeryPerUser;

    public DashboardBean()
    {
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        try
        {
            // for now just hard coded to use Francesca's course
            selectedCourse = getLearnweb().getCourseManager().getCourseById(1245);

            List<Integer> selectedUserIds = selectedCourse.getUserIds();

            DashboardManager dashboardManager = getLearnweb().getDashboardManager();

            glossaryFieldSummeryPerUser = dashboardManager.getGlossaryFieldSummeryPerUser(selectedUserIds, startDate, endDate);
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }

    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;

        setPreference(PREFERENCE_STARTDATE, Long.toString(startDate.getTime()));
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;

        setPreference(PREFERENCE_ENDDATE, Long.toString(endDate.getTime()));
    }

    public List<GlossaryFieldSummery> getGlossaryFieldSummeryPerUser()
    {
        return glossaryFieldSummeryPerUser;
    }

}
