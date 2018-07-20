package de.l3s.clustering;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.PieChartModel;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.admin.AdminNotificationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.Sql;

@Named
@SessionScoped
public class UserAssessmentBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 5446061086847252293L;

    /**
     * User Assessment Bean
     * 
     * @author Alana Morais
     */

    private static final Logger log = Logger.getLogger(UserAssessmentBean.class);

    //Form variables
    private List<User> group1 = new ArrayList<>();
    private List<User> group2 = new ArrayList<>();
    private List<User> group3 = new ArrayList<>();
    private List<Course> courses = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private List<Group> groups = new ArrayList<>();
    private Map<String, String> infoDetailUser = new HashMap<>();
    private List<Metric> listAClass;

    private String selectCourse1;
    private String selectCourse20;
    private String selectCourse20_name;
    private String selectCourse21;
    private String selectCourse21_name;
    private String selectCourse3;
    private String selectedUser;
    private String selectedGroup1;
    private String selectedGroup1_name;
    private String selectedGroup2;
    private String selectedGroup2_name;
    private String selectChart1;
    private String selectChart2;
    private String selectChart3;
    private String phaseLW0;
    private String phaseLW;
    private String phaseLW2;

    //variables Feedback
    private String title, message;
    private String classFeedback, groupFeedback, userFeedback;
    private String[] listUserGroup, listUserClass;

    //Control variables
    private boolean showb3, showl3, showp3, showd3 = false;
    private boolean showb2, showl2, showp2 = false;
    private boolean showb1, showl1, showp1 = false;

    //Plots variables
    private BarChartModel barModel0 = new BarChartModel();
    private BarChartModel barModel = new BarChartModel();
    private BarChartModel barModel2 = new BarChartModel();
    private PieChartModel pieModel00 = new PieChartModel();
    private PieChartModel pieModel01 = new PieChartModel();
    private PieChartModel pieModel = new PieChartModel();
    private PieChartModel pieModel20 = new PieChartModel();
    private PieChartModel pieModel21 = new PieChartModel();
    private LineChartModel lineModel0 = new LineChartModel();
    private LineChartModel lineModel = new LineChartModel();
    private LineChartModel lineModel2 = new LineChartModel();

    public UserAssessmentBean()
    {
        this.barModel = null;
        this.pieModel = null;
        this.lineModel = null;
        this.barModel2 = null;
        this.pieModel20 = null;
        this.pieModel21 = null;
        this.lineModel2 = null;
        this.barModel0 = null;
        this.pieModel00 = null;
        this.pieModel01 = null;
        this.lineModel0 = null;
    }

    @PostConstruct
    public void init()
    {
        clearVariables();
        //Show the class on menu
        this.selectClasses();
    }

    public void clearVariables()
    {
        group1 = null;
        group2 = null;
        group3 = null;
        infoDetailUser = null;
        listAClass = null;
        selectCourse1 = null;
        selectCourse20 = null;
        selectCourse20_name = null;
        selectCourse21 = null;
        selectCourse21_name = null;
        selectCourse3 = null;
        selectedUser = null;
        selectedGroup1 = null;
        selectedGroup1_name = null;
        selectedGroup2 = null;
        selectedGroup2_name = null;
        selectChart1 = null;
        selectChart2 = null;
        selectChart3 = null;
        phaseLW0 = null;
        phaseLW = null;
        phaseLW2 = null;
        title = null;
        message = null;
        classFeedback = null;
        groupFeedback = null;
        userFeedback = null;
        listUserGroup = null;
        listUserClass = null;
        showb3 = false;
        showl3 = false;
        showp3 = false;
        showd3 = false;
        showb2 = false;
        showl2 = false;
        showp2 = false;
        showb1 = false;
        showl1 = false;
        showp1 = false;
    }

    /* 
    * Method changes the users list after the class selection on form.
    * */
    public void changeCourse()
    {
        int idCourse = Integer.valueOf(this.selectCourse3);
        try
        {
            UserManager usm = getLearnweb().getUserManager();
            this.setUsers(removeAdminUsers(usm.getUsersByCourseId(idCourse)));
            this.setShowb3(false);
            this.setShowl3(false);
            this.setShowp3(false);
            this.setShowd3(false);
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    /*
     * Method changes the groups list after the class selection on form.
     * */
    public void changeGroup()
    {
        int idCourse = Integer.valueOf(this.selectCourse1);
        try
        {
            GroupManager usm = getLearnweb().getGroupManager();
            this.groups = usm.getGroupsByCourseId(idCourse);
            this.setShowb1(false);
            this.setShowl1(false);
            this.setShowp1(false);
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    /*
     * Get name of a specific course name according its id
     * */
    public String getNameCourse(String idCourse)
    {
        try
        {
            String courseName = (String) Sql.getSingleResult("SELECT title FROM lw_course WHERE course_id=" + idCourse);
            return courseName;
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
            return null;
        }
    }

    /*
     * Get name of a specific group name according its id
     * */
    public String getNameGroup(String idGroup)
    {
        try
        {
            //log.debug("idGroup: " + idGroup);
            idGroup = idGroup.trim();
            String name = (String) Sql.getSingleResult("SELECT title FROM lw_group WHERE group_id=" + idGroup);
            return name;
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
            return null;
        }
    }

    /*
     * Get name of a specific user name according his id
     * */
    public String getNameUser(String idUser)
    {
        try
        {
            String userName = (String) Sql.getSingleResult("SELECT username FROM lw_user WHERE user_id=" + idUser);
            return userName;
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
            return null;
        }
    }

    /*
     * Method call the other functions to create a user chart and set the control variables
     * */
    public void generateUserChart()
    {
        //log.debug("generateUserChart");

        if((!Objects.equals(this.selectChart3, "")) && (!Objects.equals(this.selectedUser, "")) && (this.selectChart3.compareTo("bar") == 0) && (this.phaseLW2 != null))
        {
            this.setShowb3(true);
            this.setShowl3(false);
            this.setShowp3(false);
            this.setShowd3(true);
        }
        if((!Objects.equals(this.selectChart3, "")) && (!Objects.equals(this.selectedUser, "")) && (this.selectChart3.compareTo("line") == 0) && (this.phaseLW2 != null))
        {
            this.setShowl3(true);
            this.setShowb3(false);
            this.setShowp3(false);
            this.setShowd3(true);
        }
        if((!Objects.equals(this.selectChart3, "")) && (!Objects.equals(this.selectedUser, "")) && (this.selectChart3.compareTo("pie") == 0) && (this.phaseLW2 != null))
        {
            this.setShowp3(true);
            this.setShowl3(false);
            this.setShowb3(false);
            this.setShowd3(false);
        }

        if((this.selectChart3.compareTo("bar") == 0) && this.showb3)
        {
            this.listAClass = new ArrayList<>();
            HashMap<String, Long> list = new HashMap<>();
            list = this.searchActivities();
            String userName = getNameUser(this.selectedUser);
            //log.debug(list);
            this.selectInteractionMeanClass();
            String title = "Details about " + userName + "'s interactions during the " + this.phaseLW2 + " phase.";
            this.initBarModelUser(title, "User Activities", list, "Activities", "Number of Interactions");
        }
        if((this.selectChart3.compareTo("line") == 0) && this.showl3)
        {
            this.listAClass = new ArrayList<>();
            HashMap<String, Long> list = new HashMap<>();
            list = this.searchActivities();
            String userName = getNameUser(this.selectedUser);
            //log.debug(list);
            String title = "Details about " + userName + "'s interactions during the " + this.phaseLW2 + " phase.";
            this.initLineModelUser(title, "User Activities", list, "Activities", "Number of Interactions");
            this.selectInteractionMeanClass();
        }
        if((this.selectChart3.compareTo("pie") == 0) && this.showp3)
        {
            this.listAClass = new ArrayList<>();
            HashMap<String, Long> list = new HashMap<>();
            list = this.searchActivities();
            String userName = getNameUser(this.selectedUser);
            //log.debug(list);
            String title = "Details about " + userName + "'s interactions during the " + this.phaseLW2 + " phase.";
            this.initPieModelUser(title, "User Activities", list);
            this.selectInteractionMeanClass();
        }
    }

    /*
     * Method call the other functions to create the comparative group charts and set the control variables
     * */
    public void generateComparativeGraphGroups()
    {
        //log.debug("generateComparativeGraphGraphs");
        //log.debug("Group 1:" + this.selectedGroup1);
        //log.debug("Group 2:" + this.selectedGroup2);

        if((!Objects.equals(this.selectChart1, "")) && (!Objects.equals(this.selectedGroup1, "")) && (!Objects.equals(this.selectedGroup2, "")) && (this.selectChart1.compareTo("bar") == 0))
        {
            this.setShowb1(true);
            this.setShowl1(false);
            this.setShowp1(false);
        }
        if((!Objects.equals(this.selectChart1, "")) && (!Objects.equals(this.selectedGroup1, "")) && (!Objects.equals(this.selectedGroup2, "")) && (this.selectChart1.compareTo("line") == 0))
        {
            this.setShowl1(true);
            this.setShowb1(false);
            this.setShowp1(false);
        }
        if((!Objects.equals(this.selectChart1, "")) && (!Objects.equals(this.selectedGroup1, "")) && (!Objects.equals(this.selectedGroup2, "")) && (this.selectChart1.compareTo("pie") == 0))
        {
            this.setShowp1(true);
            this.setShowl1(false);
            this.setShowb1(false);
        }

        if((this.selectChart1.compareTo("bar") == 0) && this.showb1)
        {
            HashMap<String, Long> list = new HashMap<>();
            HashMap<String, Long> list2 = new HashMap<>();
            list = this.searchGeneralActivitiesbyGroup(this.selectedGroup1);
            list2 = this.searchGeneralActivitiesbyGroup(this.selectedGroup2);
            //log.debug(list);
            //log.debug(list2);
            String title = "Comparison among activities of " + getNameGroup(this.selectedGroup1) + " and " + getNameGroup(this.selectedGroup2) + " groups during the " + this.phaseLW0 + " phase.";
            this.barModel0 = this.initBarComparative(title, getNameGroup(this.selectedGroup1), getNameGroup(this.selectedGroup2), list, list2, "Activities", "Number of Interactions");
        }
        if((this.selectChart1.compareTo("line") == 0) && this.showl1)
        {
            HashMap<String, Long> list = new HashMap<>();
            HashMap<String, Long> list2 = new HashMap<>();
            list = this.searchGeneralActivitiesbyGroup(this.selectedGroup1);
            list2 = this.searchGeneralActivitiesbyGroup(this.selectedGroup2);
            //log.debug(list);
            //log.debug(list2);
            String title = "Comparison among activities of " + getNameGroup(this.selectedGroup1) + " and " + getNameGroup(this.selectedGroup2) + " groups during the " + this.phaseLW0 + " phase.";
            this.lineModel0 = this.initLineComparative(title, getNameGroup(this.selectedGroup1), getNameGroup(this.selectedGroup2), list, list2, "Activities", "Number of Interactions");
        }
        if((this.selectChart1.compareTo("pie") == 0) && this.showp1)
        {
            HashMap<String, Long> list = new HashMap<>();
            HashMap<String, Long> list2 = new HashMap<>();
            list = this.searchGeneralActivitiesbyGroup(this.selectedGroup1);
            list2 = this.searchGeneralActivitiesbyGroup(this.selectedGroup2);
            //log.debug(list);
            //log.debug(list2);
            List<PieChartModel> pies = this.initPieComparative(getNameGroup(this.selectedGroup1), getNameGroup(this.selectedGroup2), list, list2, this.phaseLW0);
            this.pieModel00 = pies.get(0);
            this.pieModel01 = pies.get(1);
        }
    }

    /*
     * Method call the other functions to create the comparative class charts and set the control variables
     * */
    public void generateComparativeGraph()
    {
        //log.debug("generateComparativeGraph");

        if((!Objects.equals(this.selectChart2, "")) && (!Objects.equals(this.selectCourse20, "")) && (!Objects.equals(this.selectCourse21, "")) && (this.selectChart2.compareTo("bar") == 0))
        {
            this.setShowb2(true);
            this.setShowl2(false);
            this.setShowp2(false);
        }
        if((!Objects.equals(this.selectChart2, "")) && (!Objects.equals(this.selectCourse20, "")) && (!Objects.equals(this.selectCourse21, "")) && (this.selectChart2.compareTo("line") == 0))
        {
            this.setShowl2(true);
            this.setShowb2(false);
            this.setShowp2(false);
        }
        if((!Objects.equals(this.selectChart2, "")) && (!Objects.equals(this.selectCourse20, "")) && (!Objects.equals(this.selectCourse21, "")) && (this.selectChart2.compareTo("pie") == 0))
        {
            this.setShowp2(true);
            this.setShowl2(false);
            this.setShowb2(false);
        }

        if((this.selectChart2.compareTo("bar") == 0) && this.showb2)
        {
            HashMap<String, Long> list = new HashMap<>();
            HashMap<String, Long> list2 = new HashMap<>();
            list = this.searchGeneralActivities(this.selectCourse20, this.phaseLW);
            list2 = this.searchGeneralActivities(this.selectCourse21, this.phaseLW);
            String c1 = getNameCourse(this.selectCourse20);
            String c2 = getNameCourse(this.selectCourse21);
            //log.debug(list);
            //log.debug(list2);
            String title = "Comparison among activities of " + c1 + " and " + c2 + " classes during the " + this.phaseLW + " phase.";
            this.barModel2 = this.initBarComparative(title, c1, c2, list, list2, "Activities", "Number of Interactions");
        }
        if((this.selectChart2.compareTo("line") == 0) && this.showl2)
        {
            HashMap<String, Long> list = new HashMap<>();
            HashMap<String, Long> list2 = new HashMap<>();
            list = this.searchGeneralActivities(this.selectCourse20, this.phaseLW);
            list2 = this.searchGeneralActivities(this.selectCourse21, this.phaseLW);
            String c1 = getNameCourse(this.selectCourse20);
            String c2 = getNameCourse(this.selectCourse21);
            //log.debug(list);
            //log.debug(list2);
            String title = "Comparison among activities of " + c1 + " and " + c2 + " classes during the " + this.phaseLW + " phase.";
            this.lineModel2 = this.initLineComparative(title, c1, c2, list, list2, "Activities", "Number of Interactions");
        }
        if((this.selectChart2.compareTo("pie") == 0) && this.showp2)
        {
            HashMap<String, Long> list = new HashMap<>();
            HashMap<String, Long> list2 = new HashMap<>();
            list = this.searchGeneralActivities(this.selectCourse20, this.phaseLW);
            list2 = this.searchGeneralActivities(this.selectCourse21, this.phaseLW);
            String c1 = getNameCourse(this.selectCourse20);
            String c2 = getNameCourse(this.selectCourse21);
            //log.debug(list);
            //log.debug(list2);
            List<PieChartModel> pies = this.initPieComparative(c1, c2, list, list2, this.phaseLW);
            this.pieModel20 = pies.get(0);
            this.pieModel21 = pies.get(1);
        }
    }

    public void identifyGroups()
    {
        //Exportar em um Json os dados
        //Kmeans algorithm
        //Pode ser uma outra classe
    }

    /*
     * Method searches and returns the activities by a specific user 
     * */
    public HashMap<String, Long> searchActivities()
    {
        HashMap<String, Long> listActivities = new HashMap<>();
        try
        {
            /* Searching(s) - Open resource - Add resource - Delete resource - Create group - Group joining - Group leaving*/
            if(this.phaseLW2.compareTo("Search and exploration") == 0)
            {
                Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=5");
                Long addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=15");
                Long delResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=14");
                Long openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=3");
                Long createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=7");
                Long groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=6");
                Long groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=8");
                //Add new activities here

                listActivities.put("Searching", searching);
                listActivities.put("Open Resource", openResource);
                listActivities.put("Add Resource", addResource);
                listActivities.put("Delete Resource", delResource);
                listActivities.put("Create Group", createGroup);
                listActivities.put("Group Joining", groupJoining);
                listActivities.put("Group Leaving", groupLeaving);
                //Add new activities here
            }

            if(this.phaseLW2.compareTo("Annotation and description") == 0)
            {
                Long rating = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=1");
                Long tagging = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=0");
                Long editResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=19");
                Long commenting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=2");
                Long deleteComments = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=17");
                //Add new activities here

                listActivities.put("Rating", rating);
                listActivities.put("Tagging", tagging);
                listActivities.put("Comments", commenting);
                listActivities.put("Edit Resource", editResource);
                listActivities.put("Deleting Comments", deleteComments);
                //Add new activities here
            }
            return listActivities;
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
            return null;
        }
    }

    /*
     * Method searches and returns the activities by class
     * */
    public HashMap<String, Long> searchGeneralActivities(String classe, String phase)
    {
        // SELECT * FROM `lw_user_log` A1 INNER JOIN `lw_user_course` A2 ON A1.user_id = A2.user_id WHERE A2.course_id=640 AND A1.action=X
        HashMap<String, Long> listGeneralActivities = new HashMap<>();

        if(phase.compareTo("Search and exploration") == 0)
        {
            try
            {
                /* Searching(s) - Open resource - Add resource - Delete resource - Create group - Group joining - Group leaving*/
                Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=5");
                Long openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=3");
                Long addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=15");
                Long deleteResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=14");
                Long createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=7");
                Long groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=6");
                Long groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=8");
                //Add new activities here

                listGeneralActivities.put("Searching", searching);
                listGeneralActivities.put("Open Resource", openResource);
                listGeneralActivities.put("Add Resource", addResource);
                listGeneralActivities.put("Delete Resource", deleteResource);
                listGeneralActivities.put("Create Group", createGroup);
                listGeneralActivities.put("Group Joining", groupJoining);
                listGeneralActivities.put("Group Leaving", groupLeaving);
                //Add new activities here
            }
            catch(SQLException e)
            {
                log.error("unhandled error", e);
                return null;
            }
        }

        if(phase.compareTo("Annotation and description") == 0)
        {
            try
            {
                Long rating = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=1");
                Long tagging = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=0");
                Long editResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=19");
                Long commenting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=2");
                Long deleteComments = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=17");
                //Add new activities here

                listGeneralActivities.put("Rating", rating);
                listGeneralActivities.put("Tagging", tagging);
                listGeneralActivities.put("Comments", commenting);
                listGeneralActivities.put("Edit Resource", editResource);
                listGeneralActivities.put("Deleting Comments", deleteComments);
                //Add new activities here
            }
            catch(SQLException e)
            {
                log.error("unhandled error", e);
                return null;
            }
        }
        return listGeneralActivities;

    }

    /*
     * Method searches and returns the activities by group
     * */
    public HashMap<String, Long> searchGeneralActivitiesbyGroup(String group)
    {
        HashMap<String, Long> listActivitiesGroup = new HashMap<>();
        try
        {
            /* Searching(s) - Open resource - Add resource - Delete resource - Create group - Group joining - Group leaving*/
            if(this.phaseLW0.compareTo("Search and exploration") == 0)
            {
                Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=5");
                Long addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=15");
                Long delResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=14");
                Long openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=3");
                Long createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=7");
                Long groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=6");
                Long groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=8");
                //Add new activities here

                listActivitiesGroup.put("Searching", searching);
                listActivitiesGroup.put("Open Resource", openResource);
                listActivitiesGroup.put("Add Resource", addResource);
                listActivitiesGroup.put("Delete Resource", delResource);
                listActivitiesGroup.put("Create Group", createGroup);
                listActivitiesGroup.put("Group Joining", groupJoining);
                listActivitiesGroup.put("Group Leaving", groupLeaving);
                //Add new activities here

            }

            if(this.phaseLW0.compareTo("Annotation and description") == 0)
            {
                Long rating = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=1");
                Long tagging = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=0");
                Long editResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=19");
                Long commenting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=2");
                Long deleteComments = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=17");
                //Add new activities here

                listActivitiesGroup.put("Rating", rating);
                listActivitiesGroup.put("Tagging", tagging);
                listActivitiesGroup.put("Comments", commenting);
                listActivitiesGroup.put("Edit Resource", editResource);
                listActivitiesGroup.put("Deleting Comments", deleteComments);
                //Add new activities here
            }

            return listActivitiesGroup;

        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
            return null;
        }
    }

    /*
     * Method searches and returns the all classes
     * */
    public void selectClasses()
    {
        if(getUser() == null)
            return;
        try
        {
            courses = getUser().getCourses();
            //log.debug(courses.toString());
        }
        catch(SQLException e1)
        {
            addFatalMessage(e1);
        }
    }

    /*
     * Method calculates the mean of interactions in a specific class
     * */
    public void selectInteractionMeanClass()
    {
        //log.debug("selectInteractionMeanClass");
        try
        {
            //SELECT count(*) FROM lw_user_course A1 INNER JOIN lw_user A2 ON A1.user_id = A2.user_id WHERE A1.course_id=640 AND ((A2.is_admin=0) AND (A2.is_moderator=0))
            Long numStudents = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_course A1 INNER JOIN lw_user A2 ON A1.user_id = A2.user_id WHERE A1.course_id=" + this.selectCourse3 + " AND ((A2.is_admin=0) AND (A2.is_moderator=0))");

            if(this.phaseLW2.compareTo("Search and exploration") == 0)
            {
                Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON (A1.user_id = A2.user_id) WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=5");
                Long openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON (A1.user_id = A2.user_id) WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=3");
                Long addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON (A1.user_id = A2.user_id) WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=15");
                Long deleteResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON (A1.user_id = A2.user_id) WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=14");
                Long createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON (A1.user_id = A2.user_id) WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=7");
                Long groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON (A1.user_id = A2.user_id) WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=6");
                Long groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON (A1.user_id = A2.user_id) WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=8");//Add new activities here

                Metric a = new Metric("Busca", "", "Searching", roundValue(searching.doubleValue() / numStudents));
                listAClass.add(a);

                Metric a2 = new Metric("Acesso à Recurso", "", "Opening Resource", roundValue(openResource.doubleValue() / numStudents));
                listAClass.add(a2);

                Metric a3 = new Metric("Adição de Recurso", "", "Adding Resource", roundValue(addResource.doubleValue() / numStudents));
                listAClass.add(a3);

                Metric a4 = new Metric("Remoção de Recurso", "", "Delete Resource", roundValue(deleteResource.doubleValue() / numStudents));
                listAClass.add(a4);

                Metric a5 = new Metric("Criação de Grupo", "", "Group Creating", roundValue(createGroup.doubleValue() / numStudents));
                listAClass.add(a5);

                Metric a6 = new Metric("Inscrição em Grupo", "", "Group Joining", roundValue(groupJoining.doubleValue() / numStudents));
                listAClass.add(a6);

                Metric a7 = new Metric("Saída de Grupo", "", "Group Leaving", roundValue(groupLeaving.doubleValue() / numStudents));
                listAClass.add(a7);
                //Add new activities here
            }

            if(this.phaseLW2.compareTo("Annotation and description") == 0)
            {
                Long rating = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=1");
                Long tagging = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=0");
                Long editResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=19");
                Long commenting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=2");
                Long deleteComments = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.action=17");
                //Add new activities here

                Metric a = new Metric("Classificação", "", "Rating", roundValue(rating.doubleValue() / numStudents));
                listAClass.add(a);

                Metric a1 = new Metric("Adição de Tag", "", "Tagging", roundValue(tagging.doubleValue() / numStudents));
                listAClass.add(a1);

                Metric a2 = new Metric("Comentários", "", "Comments", roundValue(commenting.doubleValue() / numStudents));
                listAClass.add(a2);

                Metric a3 = new Metric("Edição de Recurso", "", "Edit Resource", roundValue(editResource.doubleValue() / numStudents));
                listAClass.add(a3);

                Metric a4 = new Metric("Remoção de Comentários", "", "Deleting Comment", roundValue(deleteComments.doubleValue() / numStudents));
                listAClass.add(a4);
                //Add new activities here
            }
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    /*
     * Method to select the Users in the class
     * */
    public void selectListUserClass()
    {
        //log.debug("selectListUserClass");
        int idCourse = Integer.valueOf(this.classFeedback);
        //log.debug("class:" + this.classFeedback);

        UserManager usm = getLearnweb().getUserManager();
        try
        {
            List<User> l = new ArrayList<>();
            l = removeAdminUsers(usm.getUsersByCourseId(idCourse));
            this.listUserClass = new String[l.size()];
            //log.debug("lista users:" + l);

            for(int i = 0; i < l.size(); i++)
            {
                this.listUserClass[i] = String.valueOf(l.get(i).getId());
            }
            //log.debug("versao final" + this.listUserClass[0]);
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    /*
     * Method to select the Users in the class
     * */
    public void selectListUserGroup()
    {
        //log.debug("selectListUserGroup");
        int idGroup = Integer.valueOf(this.groupFeedback);
        //log.debug("Group:" + this.groupFeedback);

        UserManager usm = getLearnweb().getUserManager();
        try
        {
            List<User> l = new ArrayList<>();
            l = removeAdminUsers(usm.getUsersByGroupId(idGroup));
            this.listUserGroup = new String[l.size()];
            //log.debug("lista users:" + l);

            for(int i = 0; i < l.size(); i++)
            {
                this.listUserGroup[i] = String.valueOf(l.get(i).getId());
            }
            //log.debug("versao final" + this.listUserGroup[0]);
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    /*
     * Method sends a feedBack in the Group Perspective
     * */
    public void sendFeedback() throws IOException
    {
        //log.debug("sendFeedback");
        try
        {
            AdminNotificationBean notBean = new AdminNotificationBean();
            if(this.listUserClass != null)
            {
                notBean.setListStudents(this.listUserClass);
                //log.debug(this.listUserClass);
                this.listUserClass = null;
            }
            if(this.listUserGroup != null)
            {
                notBean.setListStudents(this.listUserGroup);
                //log.debug(this.listUserGroup);
                this.listUserGroup = null;
            }
            notBean.setTitle(this.title);
            notBean.setText(this.message);
            notBean.send2();

            this.title = "";
            this.message = "";
            this.classFeedback = null;
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            context.redirect("admin/user_assessment.jsf");
            clearVariables();
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    /*
     * Method sends a feedBack to the User
     * */
    public void sendFeedbackUser() throws IOException
    {
        //log.debug("sendFeedbackUser");
        //HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
        //log.debug("Parametro:" + request.getAttribute("selected_users"));
        try
        {
            AdminNotificationBean notBean = new AdminNotificationBean();
            notBean.setTitle(this.title);
            notBean.setText(this.message);
            notBean.send();
            this.title = "";
            this.message = "";
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            context.redirect("admin/user_assessment.jsf");
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    /**
     * Method constructed to remove admins and moderators users
     *
     * @author Alana
     * @param list
     * @return list without Admin users
     * @throws SQLException
     */
    public static List<User> removeAdminUsers(List<User> list) throws SQLException
    {
        List<User> finalList = new ArrayList<>();

        for(User user : list)
        {
            if(!user.isModerator() && !user.isAdmin())
                finalList.add(user);
        }

        return finalList;
    }

    /*
     * Round the double value
     * */
    public double roundValue(double val)
    {
        BigDecimal bd = new BigDecimal(val).setScale(2, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }

    /*
     * Methods for chart construction
     * */
    public void initLineModelUser(String text, String label, HashMap<String, Long> data, String xLabel, String yLabel)
    {
        //log.debug("initLineModelUser");
        this.lineModel = new LineChartModel();
        Long maxValue = 0L;

        ChartSeries class1 = new ChartSeries();
        class1.setLabel(label);
        for(String key : data.keySet())
        {
            Long value = data.get(key);
            class1.set(key, value);
            if(value > maxValue)
                maxValue = value;
            //log.debug("key:" + key + ", value:" + value);
        }

        this.lineModel.addSeries(class1);
        this.lineModel.setTitle(text);
        this.lineModel.setLegendPosition("e");
        this.lineModel.setAnimate(true);
        this.lineModel.setZoom(true);

        this.lineModel.getAxes().put(AxisType.X, new CategoryAxis(xLabel));
        Axis yAxis = this.lineModel.getAxis(AxisType.Y);
        yAxis.setLabel(yLabel);

        while(maxValue % 10 != 0)
            maxValue++;

        yAxis.setMin(0);
        yAxis.setMax(maxValue);

    }

    public void initBarModelUser(String text, String label, HashMap<String, Long> data, String xLabel, String yLabel)
    {
        //log.debug("initBarModelUser");
        this.barModel = new BarChartModel();
        Long maxValue = 0L;

        this.barModel.setTitle(text);
        this.barModel.setLegendPosition("e");
        this.barModel.setAnimate(true);
        this.barModel.setZoom(true);

        ChartSeries group1 = new ChartSeries();
        group1.setLabel(label);

        for(String key : data.keySet())
        {
            Long value = data.get(key);
            group1.set(key, value);
            if(value > maxValue)
                maxValue = value;
            //log.debug("key:" + key + ", value:" + value);
        }

        this.barModel.addSeries(group1);
        Axis xAxis = this.barModel.getAxis(AxisType.X);
        xAxis.setLabel(xLabel);
        Axis yAxis = this.barModel.getAxis(AxisType.Y);
        yAxis.setLabel(yLabel);

        while(maxValue % 10 != 0)
            maxValue++;

        yAxis.setMin(0);
        yAxis.setMax(maxValue);

        //if(this.barModel != null)
        //log.debug(this.barModel.toString());
    }

    public void initPieModelUser(String text, String label, HashMap<String, Long> data)
    {
        //log.debug("initPieModelUser");
        this.pieModel = new PieChartModel();

        for(String key : data.keySet())
        {
            Long value = data.get(key);
            this.pieModel.set(key, value);
            //log.debug("key:" + key + ", value:" + value);
        }
        this.pieModel.setTitle(text);
        this.pieModel.setLegendPosition("ne");
        this.pieModel.setFill(false);
        this.pieModel.setShowDataLabels(true);
        this.pieModel.setDiameter(250);
    }

    public BarChartModel initBarComparative(String text, String label, String label1, HashMap<String, Long> data, HashMap<String, Long> data2, String xLabel, String yLabel)
    {
        //log.debug("initBarComparative");
        BarChartModel b1 = new BarChartModel();
        Long maxValue = 0L;

        b1.setTitle(text);
        b1.setLegendPosition("ne");

        ChartSeries group1 = new ChartSeries();
        group1.setLabel(label);

        for(String key : data.keySet())
        {
            Long value = data.get(key);
            group1.set(key, value);
            if(value > maxValue)
                maxValue = value;
            //log.debug("key:" + key + ", value:" + value);
        }

        ChartSeries group2 = new ChartSeries();
        group2.setLabel(label1);

        for(String key2 : data2.keySet())
        {
            Long value = data2.get(key2);
            group2.set(key2, value);
            if(value > maxValue)
                maxValue = value;
            //log.debug("key:" + key2 + ", value:" + value);
        }

        b1.addSeries(group1);
        b1.addSeries(group2);
        Axis xAxis = b1.getAxis(AxisType.X);
        xAxis.setLabel(xLabel);
        Axis yAxis = b1.getAxis(AxisType.Y);
        yAxis.setLabel(yLabel);
        yAxis.setMin(0);

        while(maxValue % 10 != 0)
            maxValue++;

        yAxis.setMax(maxValue);
        b1.setAnimate(true);

        return b1;
    }

    public LineChartModel initLineComparative(String text, String label, String label1, HashMap<String, Long> data, HashMap<String, Long> data2, String xLabel, String yLabel)
    {
        //log.debug("initLineComprative");
        LineChartModel l1 = new LineChartModel();
        Long maxValue = 0L;

        ChartSeries class1 = new ChartSeries();
        class1.setLabel(label);
        for(String key : data.keySet())
        {
            Long value = data.get(key);
            class1.set(key, value);
            if(value > maxValue)
                maxValue = value;
            //log.debug("key:" + key + ", value:" + value);
        }

        ChartSeries class2 = new ChartSeries();
        class2.setLabel(label1);
        for(String key : data2.keySet())
        {
            Long value = data2.get(key);
            class2.set(key, value);
            if(value > maxValue)
                maxValue = value;
            //log.debug("key:" + key + ", value:" + value);
        }
        while(maxValue % 10 != 0)
            maxValue++;

        l1.addSeries(class1);
        l1.addSeries(class2);
        l1.setTitle(text);
        l1.setLegendPosition("e");
        l1.setShowPointLabels(true);
        l1.getAxes().put(AxisType.X, new CategoryAxis(xLabel));
        Axis yAxis = l1.getAxis(AxisType.Y);
        yAxis.setLabel(yLabel);
        yAxis.setMin(0);
        yAxis.setMax(maxValue);
        l1.setAnimate(true);
        return l1;
    }

    public List<PieChartModel> initPieComparative(String label, String label1, HashMap<String, Long> data, HashMap<String, Long> data2, String phase)
    {
        //log.debug("initPieModelComparative");
        PieChartModel p1 = new PieChartModel();
        PieChartModel p2 = new PieChartModel();

        for(String key : data.keySet())
        {
            Long value = data.get(key);
            p1.set(key, value);
            //log.debug("key:" + key + ", value:" + value);
        }

        for(String key : data2.keySet())
        {
            Long value = data2.get(key);
            p2.set(key, value);
            //log.debug("key:" + key + ", value:" + value);
        }
        String title = "Comparison among activities of " + label + " and " + label1 + " during the " + this.phaseLW + " phase. <br/><br/>" + label;
        p1.setTitle(title);
        p1.setLegendPosition("e");
        p1.setFill(false);
        p1.setShowDataLabels(true);
        p1.setDiameter(200);

        p2.setTitle(label1);
        p2.setLegendPosition("e");
        p2.setFill(false);
        p2.setShowDataLabels(true);
        p2.setDiameter(200);

        List<PieChartModel> listPie = new ArrayList<>();
        listPie.add(p1);
        listPie.add(p2);
        return listPie;

    }

    /*
     * Gets and Sets
     * */
    public List<User> getGroup1()
    {
        return group1;
    }

    public void setGroup1(List<User> group1)
    {
        this.group1 = group1;
    }

    public List<User> getGroup2()
    {
        return group2;
    }

    public void setGroup2(List<User> group2)
    {
        this.group2 = group2;
    }

    public List<User> getGroup3()
    {
        return group3;
    }

    public void setGroup3(List<User> group3)
    {
        this.group3 = group3;
    }

    public List<Course> getCourses()
    {
        return courses;
    }

    public void setCourses(List<Course> courses)
    {
        this.courses = courses;
    }

    public String getSelectCourse3()
    {
        return selectCourse3;
    }

    public void setSelectCourse3(String selectCourse3)
    {
        this.selectCourse3 = selectCourse3;
    }

    public String getSelectCourse1()
    {
        return selectCourse1;
    }

    public void setSelectCourse1(String selectCourse1)
    {
        this.selectCourse1 = selectCourse1;
    }

    public String getSelectCourse20()
    {
        return selectCourse20;
    }

    public void setSelectCourse20(String selectCourse20)
    {
        this.selectCourse20 = selectCourse20;
        this.selectCourse20_name = this.getNameCourse(this.selectCourse20);
    }

    public String getSelectedUser()
    {
        return selectedUser;
    }

    public void setSelectedUser(String selectedUser)
    {
        this.selectedUser = selectedUser;
    }

    public String getSelectChart2()
    {
        return selectChart2;
    }

    public void setSelectChart2(String selectChart2)
    {
        this.selectChart2 = selectChart2;
    }

    public String getSelectChart1()
    {
        return selectChart1;
    }

    public void setSelectChart1(String selectChart1)
    {
        this.selectChart1 = selectChart1;
    }

    public String getSelectChart3()
    {
        return selectChart3;
    }

    public void setSelectChart3(String selectChart3)
    {
        this.selectChart3 = selectChart3;
    }

    public List<User> getUsers()
    {
        return users;
    }

    public void setUsers(List<User> users)
    {
        this.users = users;
    }

    public BarChartModel getBarModel()
    {
        return barModel;
    }

    public void setBarModel(BarChartModel barModel)
    {
        this.barModel = barModel;
    }

    public PieChartModel getPieModel()
    {
        return pieModel;
    }

    public void setPieModel(PieChartModel pieModel)
    {
        this.pieModel = pieModel;
    }

    public LineChartModel getLineModel2()
    {
        return lineModel2;
    }

    public void setLineModel2(LineChartModel lineModel2)
    {
        this.lineModel2 = lineModel2;
    }

    public BarChartModel getBarModel2()
    {
        return barModel2;
    }

    public void setBarModel2(BarChartModel barModel2)
    {
        this.barModel2 = barModel2;
    }

    public PieChartModel getPieModel20()
    {
        return pieModel20;
    }

    public void setPieModel20(PieChartModel pieModel20)
    {
        this.pieModel20 = pieModel20;
    }

    public PieChartModel getPieModel21()
    {
        return pieModel21;
    }

    public void setPieModel21(PieChartModel pieModel21)
    {
        this.pieModel21 = pieModel21;
    }

    public LineChartModel getLineModel()
    {
        return lineModel;
    }

    public void setLineModel(LineChartModel lineModel)
    {
        this.lineModel = lineModel;
    }

    public Map<String, String> getInfoDetailUser()
    {
        return infoDetailUser;
    }

    public void setInfoDetailUser(Map<String, String> infoDetailUser)
    {
        this.infoDetailUser = infoDetailUser;
    }

    public String getSelectCourse21()
    {
        return selectCourse21;
    }

    public void setSelectCourse21(String selectCourse21)
    {
        this.selectCourse21 = selectCourse21;
        this.selectCourse21_name = this.getNameCourse(this.selectCourse21);
    }

    public boolean isShowb3()
    {
        return showb3;
    }

    public void setShowb3(boolean show)
    {
        this.showb3 = show;
    }

    public boolean isShowp3()
    {
        return showp3;
    }

    public void setShowp3(boolean showp3)
    {
        this.showp3 = showp3;
    }

    public boolean isShowl3()
    {
        return showl3;
    }

    public void setShowl3(boolean showl3)
    {
        this.showl3 = showl3;
    }

    public boolean isShowb2()
    {
        return showb2;
    }

    public void setShowb2(boolean showb2)
    {
        this.showb2 = showb2;
    }

    public boolean isShowl2()
    {
        return showl2;
    }

    public void setShowl2(boolean showl2)
    {
        this.showl2 = showl2;
    }

    public boolean isShowp2()
    {
        return showp2;
    }

    public boolean isShowd3()
    {
        return showd3;
    }

    public void setShowd3(boolean showd3)
    {
        this.showd3 = showd3;
    }

    public void setShowp2(boolean showp2)
    {
        this.showp2 = showp2;
    }

    public String getPhaseLW()
    {
        return phaseLW;
    }

    public void setPhaseLW(String phaseLW)
    {
        this.phaseLW = phaseLW;
    }

    public String getPhaseLW2()
    {
        return phaseLW2;
    }

    public void setPhaseLW2(String phaseLW2)
    {
        this.phaseLW2 = phaseLW2;
    }

    public BarChartModel getBarModel0()
    {
        return barModel0;
    }

    public void setBarModel0(BarChartModel barModel0)
    {
        this.barModel0 = barModel0;
    }

    public PieChartModel getPieModel00()
    {
        return pieModel00;
    }

    public void setPieModel00(PieChartModel pieModel00)
    {
        this.pieModel00 = pieModel00;
    }

    public PieChartModel getPieModel01()
    {
        return pieModel01;
    }

    public void setPieModel01(PieChartModel pieModel01)
    {
        this.pieModel01 = pieModel01;
    }

    public boolean isShowl1()
    {
        return showl1;
    }

    public void setShowl1(boolean showl1)
    {
        this.showl1 = showl1;
    }

    public boolean isShowb1()
    {
        return showb1;
    }

    public void setShowb1(boolean showb1)
    {
        this.showb1 = showb1;
    }

    public boolean isShowp1()
    {
        return showp1;
    }

    public void setShowp1(boolean showp1)
    {
        this.showp1 = showp1;
    }

    public List<Group> getGroups()
    {
        return groups;
    }

    public void setGroups(List<Group> groups)
    {
        this.groups = groups;
    }

    public String getSelectedGroup1()
    {
        return selectedGroup1;
    }

    public void setSelectedGroup1(String selectedGroup1)
    {
        this.selectedGroup1 = selectedGroup1;
        this.selectedGroup1_name = this.getNameGroup(this.selectedGroup1);
    }

    public String getSelectedGroup2()
    {
        return selectedGroup2;
    }

    public void setSelectedGroup2(String selectedGroup2)
    {
        this.selectedGroup2 = selectedGroup2;
        this.selectedGroup2_name = this.getNameGroup(this.selectedGroup2);
    }

    public String getPhaseLW0()
    {
        return phaseLW0;
    }

    public void setPhaseLW0(String phaseLW0)
    {
        this.phaseLW0 = phaseLW0;
    }

    public LineChartModel getLineModel0()
    {
        return lineModel0;
    }

    public void setLineModel0(LineChartModel lineModel0)
    {
        this.lineModel0 = lineModel0;
    }

    public List<Metric> getListAClass()
    {
        return listAClass;
    }

    public void setListAClass(List<Metric> listAClass)
    {
        this.listAClass = listAClass;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getClassFeedback()
    {
        return classFeedback;
    }

    public void setClassFeedback(String classFeedback)
    {
        this.classFeedback = classFeedback;
    }

    public String getGroupFeedback()
    {
        return groupFeedback;
    }

    public void setGroupFeedback(String groupFeedback)
    {
        this.groupFeedback = groupFeedback;
    }

    public String getUserFeedback()
    {
        return userFeedback;
    }

    public void setUserFeedback(String userFeedback)
    {
        this.userFeedback = userFeedback;
    }

    public String getSelectCourse20_name()
    {
        return selectCourse20_name;
    }

    public void setSelectCourse20_name(String selectCourse20_name)
    {
        this.selectCourse20_name = selectCourse20_name;
    }

    public String getSelectCourse21_name()
    {
        return selectCourse21_name;
    }

    public void setSelectCourse21_name(String selectCourse21_name)
    {
        this.selectCourse21_name = selectCourse21_name;
    }

    public String getSelectedGroup1_name()
    {
        return selectedGroup1_name;
    }

    public void setSelectedGroup1_name(String selectedGroup1_name)
    {
        this.selectedGroup1_name = selectedGroup1_name;
    }

    public String getSelectedGroup2_name()
    {
        return selectedGroup2_name;
    }

    public void setSelectedGroup2_name(String selectedGroup2_name)
    {
        this.selectedGroup2_name = selectedGroup2_name;
    }

    public String[] getListUserGroup()
    {
        return listUserGroup;
    }

    public void setListUserGroup(String[] listUserGroup)
    {
        this.listUserGroup = listUserGroup;
    }

    public String[] getListUserClass()
    {
        return listUserClass;
    }

    public void setListUserClass(String[] listUserClass)
    {
        this.listUserClass = listUserClass;
    }

}
