package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.PieChartModel;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.GroupManager;
import de.l3s.learnweb.User;
import de.l3s.learnweb.UserManager;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.util.Sql;

@ManagedBean
@SessionScoped
public class UserAssessmentBean extends ApplicationBean implements Serializable
{
    /**
     * User Assessment Bean
     * 
     * @author Alana Morais
     */

    private static final long serialVersionUID = 1L;
    private List<User> group1 = new ArrayList<User>();
    private List<User> group2 = new ArrayList<User>();
    private List<User> group3 = new ArrayList<User>();
    private List<Course> courses = new ArrayList<Course>();
    private List<User> users = new ArrayList<User>();
    private List<Group> groups = new ArrayList<Group>();

    private Map<String, String> infoDetailUser = new HashMap<String, String>();

    private String selectCourse1;
    private String selectCourse20;
    private String selectCourse21;
    private String selectCourse3;
    private String selectedUser;
    private String selectedGroup1;
    private String selectedGroup2;
    private String selectChart1;
    private String selectChart2;
    private String selectChart3;
    private String phaseLW0;
    private String phaseLW;
    private String phaseLW2;

    private boolean showb3, showl3, showp3 = false;
    private boolean showb2, showl2, showp2 = false;
    private boolean showb1, showl1, showp1 = false;
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
	this.selectCourse1 = null;
	this.selectCourse20 = null;
	this.selectCourse21 = null;
	this.selectCourse3 = null;
	this.selectedUser = null;
	this.selectedGroup1 = null;
	this.selectedGroup2 = null;
	this.selectChart1 = null;
	this.selectChart2 = null;
	this.selectChart3 = null;
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
	this.phaseLW0 = null;
	this.phaseLW = null;
	this.phaseLW2 = null;
    }

    @PostConstruct
    public void init()
    {
	//Show the class on menu
	this.selectClasses();
    }

    public void changeCourse()
    {
	//Function responsible to change the users list after the class selection on menu.
	int idCourse = Integer.valueOf(this.selectCourse3);
	try
	{
	    UserManager usm = getLearnweb().getUserManager();
	    removeFakeUsers(usm.getUsersByCourseId(idCourse));
	    this.setShowb3(false);
	    this.setShowl3(false);
	    this.setShowp3(false);
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	}
    }

    public void changeGroup()
    {
	//Function responsible to change the groups list after the class selection on menu.
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
	    e.printStackTrace();
	}
    }

    public HashMap<String, Long> searchGeneralActivitiesbyGroup(String group)
    {
	//General Activities by Group
	HashMap<String, Long> listActivitiesGroup = new HashMap<String, Long>();
	try
	{
	    /* Searching(s) - Download- Open resource - Add resource - Delete resource - Create group - Group joining - Group leaving*/
	    if(this.phaseLW0.compareTo("searching") == 0)
	    {
		Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=5");
		Long downloading = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=32");
		Long addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=15");
		Long delResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=14");
		Long openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=3");
		Long createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=7");
		Long groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=6");
		Long groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_group_user A2 ON (A1.user_id = A2.user_id)  INNER JOIN lw_group A3 ON (A2.group_id = A3.group_id) WHERE A3.group_id=" + group + " AND A1.action=8");
		//Add new activities here

		listActivitiesGroup.put("Searching", searching);
		listActivitiesGroup.put("Downloading", downloading);
		listActivitiesGroup.put("Open Resource", openResource);
		listActivitiesGroup.put("Add Resource", addResource);
		listActivitiesGroup.put("Delete Resource", delResource);
		listActivitiesGroup.put("Create Group", createGroup);
		listActivitiesGroup.put("Group Joining", groupJoining);
		listActivitiesGroup.put("Group Leaving", groupLeaving);
		//Add new activities here

	    }

	    if(this.phaseLW0.compareTo("annotation") == 0)
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
	    e.printStackTrace();
	    return null;
	}
    }

    public HashMap<String, Long> searchGeneralActivities(String classe, String phase)
    {
	//General Activities in each class
	// SELECT * FROM `lw_user_log` A1 INNER JOIN `lw_user_course` A2 ON A1.user_id = A2.user_id WHERE A2.course_id=640 AND A1.action=X
	HashMap<String, Long> listGeneralActivities = new HashMap<String, Long>();

	if(phase.compareTo("searching") == 0)
	{
	    try
	    {
		/* Searching(s) - Download- Open resource - Add resource - Delete resource - Create group - Group joining - Group leaving*/
		Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=5");
		Long downloading = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=32");
		Long openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=3");
		Long addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=15");
		Long deleteResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=14");
		Long createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=7");
		Long groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=6");
		Long groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=8");
		//Add new activities here

		listGeneralActivities.put("Searching", searching);
		listGeneralActivities.put("Downloading", downloading);
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
		e.printStackTrace();
		return null;
	    }
	}

	if(phase.compareTo("annotation") == 0)
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
		e.printStackTrace();
		return null;
	    }
	}
	return listGeneralActivities;

    }

    public HashMap<String, Long> searchActivities()
    {
	HashMap<String, Long> listActivities = new HashMap<String, Long>();
	try
	{
	    /* Searching(s) - Download- Open resource - Add resource - Delete resource - Create group - Group joining - Group leaving*/
	    if(this.phaseLW2.compareTo("searching") == 0)
	    {
		Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=5");
		Long downloading = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=32");
		Long addResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=15");
		Long delResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=14");
		Long openResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=3");
		Long createGroup = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=7");
		Long groupJoining = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=6");
		Long groupLeaving = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=8");
		//Add new activities here

		listActivities.put("Searching", searching);
		listActivities.put("Downloading", downloading);
		listActivities.put("Open Resource", openResource);
		listActivities.put("Add Resource", addResource);
		listActivities.put("Delete Resource", delResource);
		listActivities.put("Create Group", createGroup);
		listActivities.put("Group Joining", groupJoining);
		listActivities.put("Group Leaving", groupLeaving);
		//Add new activities here
	    }

	    if(this.phaseLW2.compareTo("annotation") == 0)
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
	    e.printStackTrace();
	    return null;
	}
    }

    public HashMap<String, Double> selectInteractionMeanClass()
    {
	HashMap<String, Double> listActClass = new HashMap<String, Double>();

	try
	{
	    /* Searching(s) - Download- Open resource - Add resource - Delete resource - Create group - Group joining - Group leaving*/
	    Long numStudents = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=5");

	    if(this.phaseLW2.compareTo("searching") == 0)
	    {
		Double searching = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=5");
		Double downloading = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=32");
		Double openResource = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=3");
		Double addResource = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=15");
		Double deleteResource = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=14");
		Double createGroup = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=7");
		Double groupJoining = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=6");
		Double groupLeaving = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=8");//Add new activities here

		listActClass.put("Searching", searching / numStudents);
		listActClass.put("Downloading", downloading / numStudents);
		listActClass.put("Open Resource", openResource / numStudents);
		listActClass.put("Add Resource", addResource / numStudents);
		listActClass.put("Delete Resource", deleteResource / numStudents);
		listActClass.put("Create Group", createGroup / numStudents);
		listActClass.put("Group Joining", groupJoining / numStudents);
		listActClass.put("Group Leaving", groupLeaving / numStudents);
		//Add new activities here

	    }

	    if(this.phaseLW2.compareTo("annotation") == 0)
	    {
		Double rating = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=1");
		Double tagging = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=0");
		Double editResource = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=19");
		Double commenting = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=2");
		Double deleteComments = (Double) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectChart3 + " AND A1.action=17");
		//Add new activities here

		listActClass.put("Rating", rating / numStudents);
		listActClass.put("Tagging", tagging / numStudents);
		listActClass.put("Comments", commenting / numStudents);
		listActClass.put("Edit Resource", editResource / numStudents);
		listActClass.put("Deleting Comments", deleteComments / numStudents);
		//Add new activities here
	    }

	    return listActClass;

	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	    return null;
	}
    }

    public String getNameCourse(String idCourse)
    {
	try
	{
	    String courseName = (String) Sql.getSingleResult("SELECT title FROM lw_course WHERE course_id=" + idCourse);
	    return courseName;
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	    return null;
	}

    }

    public String getNameUser(String idUser)
    {
	try
	{
	    String userName = (String) Sql.getSingleResult("SELECT username FROM lw_user WHERE user_id=" + idUser);
	    return userName;
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	    return null;
	}

    }

    public String getNameGroup(String idGroup)
    {
	try
	{
	    System.out.println("idGroup: " + idGroup);
	    idGroup = idGroup.trim();
	    String name = (String) Sql.getSingleResult("SELECT title FROM lw_group WHERE group_id=" + idGroup);
	    return name;
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	    return null;
	}

    }

    public void generateUserChart()
    {
	System.out.println("generateUserChart");

	if((this.selectChart3 != "") && (this.selectedUser != "") && (this.selectChart3.compareTo("bar") == 0))
	{
	    this.setShowb3(true);
	    this.setShowl3(false);
	    this.setShowp3(false);
	}
	if((this.selectChart3 != "") && (this.selectedUser != "") && (this.selectChart3.compareTo("line") == 0))
	{
	    this.setShowl3(true);
	    this.setShowb3(false);
	    this.setShowp3(false);
	}
	if((this.selectChart3 != "") && (this.selectedUser != "") && (this.selectChart3.compareTo("pie") == 0))
	{
	    this.setShowp3(true);
	    this.setShowl3(false);
	    this.setShowb3(false);
	}

	if((this.selectChart3.compareTo("bar") == 0) && (this.showb3 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    list = this.searchActivities();
	    String userName = getNameUser(this.selectedUser);
	    System.out.println(list);
	    this.initBarModelUser("Details of " + userName, "User Activities", list, "Activities", "Number of Interactions");
	}

	if((this.selectChart3.compareTo("line") == 0) && (this.showl3 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    list = this.searchActivities();
	    String userName = getNameUser(this.selectedUser);
	    System.out.println(list);
	    this.initLineModelUser("Details of " + userName, "User Activities", list, "Activities", "Number of Interactions");
	}

	if((this.selectChart3.compareTo("pie") == 0) && (this.showp3 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    list = this.searchActivities();
	    String userName = getNameUser(this.selectedUser);
	    System.out.println(list);
	    this.initPieModelUser("Details of " + userName, "User Activities", list);
	}
    }

    public void generateComparativeGraphGroups()
    {
	System.out.println("generateComparativeGraphGraphs");
	System.out.println("Group 1:" + this.selectedGroup1);
	System.out.println("Group 2:" + this.selectedGroup2);

	if((this.selectChart1 != "") && (this.selectedGroup1 != "") && (this.selectedGroup2 != "") && (this.selectChart1.compareTo("bar") == 0))
	{
	    this.setShowb1(true);
	    this.setShowl1(false);
	    this.setShowp1(false);
	}
	if((this.selectChart1 != "") && (this.selectedGroup1 != "") && (this.selectedGroup2 != "") && (this.selectChart1.compareTo("line") == 0))
	{
	    this.setShowl1(true);
	    this.setShowb1(false);
	    this.setShowp1(false);
	}
	if((this.selectChart1 != "") && (this.selectedGroup1 != "") && (this.selectedGroup2 != "") && (this.selectChart1.compareTo("pie") == 0))
	{
	    this.setShowp1(true);
	    this.setShowl1(false);
	    this.setShowb1(false);
	}

	if((this.selectChart1.compareTo("bar") == 0) && (this.showb1 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    HashMap<String, Long> list2 = new HashMap<String, Long>();
	    list = this.searchGeneralActivitiesbyGroup(this.selectedGroup1);
	    list2 = this.searchGeneralActivitiesbyGroup(this.selectedGroup2);
	    System.out.println(list);
	    System.out.println(list2);
	    this.barModel0 = this.initBarComparative("Comparison between " + getNameGroup(this.selectedGroup1) + " and " + getNameGroup(this.selectedGroup2) + " groups", getNameGroup(this.selectedGroup1), getNameGroup(this.selectedGroup2), list, list2, "Activities",
		    "Number of Interactions");
	}

	if((this.selectChart1.compareTo("line") == 0) && (this.showl1 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    HashMap<String, Long> list2 = new HashMap<String, Long>();
	    list = this.searchGeneralActivitiesbyGroup(this.selectedGroup1);
	    list2 = this.searchGeneralActivitiesbyGroup(this.selectedGroup2);
	    System.out.println(list);
	    System.out.println(list2);
	    this.lineModel0 = this.initLineComparative("Comparison between " + getNameGroup(this.selectedGroup1) + " and " + getNameGroup(this.selectedGroup2) + " groups", getNameGroup(this.selectedGroup1), getNameGroup(this.selectedGroup2), list, list2, "Activities",
		    "Number of Interactions");
	}

	if((this.selectChart1.compareTo("pie") == 0) && (this.showp1 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    HashMap<String, Long> list2 = new HashMap<String, Long>();
	    list = this.searchGeneralActivitiesbyGroup(this.selectedGroup1);
	    list2 = this.searchGeneralActivitiesbyGroup(this.selectedGroup2);
	    System.out.println(list);
	    System.out.println(list2);
	    List<PieChartModel> pies = this.initPieComparative(getNameGroup(this.selectedGroup1), getNameGroup(this.selectedGroup2), list, list2);
	    this.pieModel00 = pies.get(0);
	    this.pieModel01 = pies.get(1);
	}
    }

    public void generateComparativeGraph()
    {
	System.out.println("generateComparativeGraph");

	if((this.selectChart2 != "") && (this.selectCourse20 != "") && (this.selectCourse21 != "") && (this.selectChart2.compareTo("bar") == 0))
	{
	    this.setShowb2(true);
	    this.setShowl2(false);
	    this.setShowp2(false);
	}
	if((this.selectChart2 != "") && (this.selectCourse20 != "") && (this.selectCourse21 != "") && (this.selectChart2.compareTo("line") == 0))
	{
	    this.setShowl2(true);
	    this.setShowb2(false);
	    this.setShowp2(false);
	}
	if((this.selectChart2 != "") && (this.selectCourse20 != "") && (this.selectCourse21 != "") && (this.selectChart2.compareTo("pie") == 0))
	{
	    this.setShowp2(true);
	    this.setShowl2(false);
	    this.setShowb2(false);
	}

	if((this.selectChart2.compareTo("bar") == 0) && (this.showb2 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    HashMap<String, Long> list2 = new HashMap<String, Long>();
	    list = this.searchGeneralActivities(this.selectCourse20, this.phaseLW);
	    list2 = this.searchGeneralActivities(this.selectCourse21, this.phaseLW);
	    String c1 = getNameCourse(this.selectCourse20);
	    String c2 = getNameCourse(this.selectCourse21);
	    System.out.println(list);
	    System.out.println(list2);
	    this.barModel2 = this.initBarComparative("Comparison between " + c1 + " and " + c2 + " class", c1, c2, list, list2, "Activities", "Number of Interactions");
	}

	if((this.selectChart2.compareTo("line") == 0) && (this.showl2 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    HashMap<String, Long> list2 = new HashMap<String, Long>();
	    list = this.searchGeneralActivities(this.selectCourse20, this.phaseLW);
	    list2 = this.searchGeneralActivities(this.selectCourse21, this.phaseLW);
	    String c1 = getNameCourse(this.selectCourse20);
	    String c2 = getNameCourse(this.selectCourse21);
	    System.out.println(list);
	    System.out.println(list2);
	    this.lineModel2 = this.initLineComparative("Comparison between " + c1 + " and " + c2 + " class", c1, c2, list, list2, "Activities", "Number of Interactions");
	}

	if((this.selectChart2.compareTo("pie") == 0) && (this.showp2 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    HashMap<String, Long> list2 = new HashMap<String, Long>();
	    list = this.searchGeneralActivities(this.selectCourse20, this.phaseLW);
	    list2 = this.searchGeneralActivities(this.selectCourse21, this.phaseLW);
	    String c1 = getNameCourse(this.selectCourse20);
	    String c2 = getNameCourse(this.selectCourse21);
	    System.out.println(list);
	    System.out.println(list2);
	    List<PieChartModel> pies = this.initPieComparative(c1, c2, list, list2);
	    this.pieModel20 = pies.get(0);
	    this.pieModel21 = pies.get(1);
	}
    }

    public void identifyGroups()
    {
	//Kmeans algorithm
	//Pode ser uma outra classe
    }

    public void initLineModelUser(String text, String label, HashMap<String, Long> data, String xLabel, String yLabel)
    {
	System.out.println("initLineModelUser");
	this.lineModel = new LineChartModel();
	Long maxValue = 0l;

	ChartSeries class1 = new ChartSeries();
	class1.setLabel(label);
	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    class1.set(key, value);
	    if(value > maxValue)
		maxValue = value;
	    System.out.println("key:" + key + ", value:" + value);
	}

	this.lineModel.addSeries(class1);
	this.lineModel.setTitle(text);
	this.lineModel.setLegendPosition("e");
	this.lineModel.setShowPointLabels(true);
	this.lineModel.getAxes().put(AxisType.X, new CategoryAxis(xLabel));
	Axis yAxis = this.lineModel.getAxis(AxisType.Y);
	yAxis.setLabel(yLabel);

	while(maxValue % 10 != 0)
	    maxValue++;

	yAxis.setMin(0);
	yAxis.setMax(maxValue);
	this.lineModel.setAnimate(true);

    }

    public void initBarModelUser(String text, String label, HashMap<String, Long> data, String xLabel, String yLabel)
    {
	System.out.println("initBarModelUser");
	this.barModel = new BarChartModel();
	Long maxValue = 0l;

	this.barModel.setTitle(text);
	this.barModel.setLegendPosition("e");

	ChartSeries group1 = new ChartSeries();
	group1.setLabel(label);

	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    group1.set(key, value);
	    if(value > maxValue)
		maxValue = value;
	    System.out.println("key:" + key + ", value:" + value);
	}

	this.barModel.addSeries(group1);
	Axis xAxis = this.barModel.getAxis(AxisType.X);
	xAxis.setLabel(xLabel);
	Axis yAxis = this.barModel.getAxis(AxisType.Y);
	yAxis.setLabel(yLabel);

	this.barModel.setAnimate(true);

	while(maxValue % 10 != 0)
	    maxValue++;

	yAxis.setMin(0);
	yAxis.setMax(maxValue);
	this.barModel.setAnimate(true);

	if(this.barModel != null)
	    System.out.println(this.barModel.toString());
    }

    public void initPieModelUser(String text, String label, HashMap<String, Long> data)
    {
	System.out.println("initPieModelUser");
	this.pieModel = new PieChartModel();

	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    this.pieModel.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	this.pieModel.setTitle(text);
	this.pieModel.setLegendPosition("ne");
	this.pieModel.setFill(false);
	this.pieModel.setShowDataLabels(true);
	this.pieModel.setDiameter(250);
    }

    public BarChartModel initBarComparative(String text, String label, String label1, HashMap<String, Long> data, HashMap<String, Long> data2, String xLabel, String yLabel)
    {
	System.out.println("initBarComparative");
	BarChartModel b1 = new BarChartModel();
	Long maxValue = 0l;

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
	    System.out.println("key:" + key + ", value:" + value);
	}

	ChartSeries group2 = new ChartSeries();
	group2.setLabel(label1);

	for(String key2 : data2.keySet())
	{
	    Long value = data2.get(key2);
	    group2.set(key2, value);
	    if(value > maxValue)
		maxValue = value;
	    System.out.println("key:" + key2 + ", value:" + value);
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
	System.out.println("initLineComprative");
	LineChartModel l1 = new LineChartModel();
	Long maxValue = 0l;

	ChartSeries class1 = new ChartSeries();
	class1.setLabel(label);
	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    class1.set(key, value);
	    if(value > maxValue)
		maxValue = value;
	    System.out.println("key:" + key + ", value:" + value);
	}

	ChartSeries class2 = new ChartSeries();
	class2.setLabel(label1);
	for(String key : data2.keySet())
	{
	    Long value = data2.get(key);
	    class2.set(key, value);
	    if(value > maxValue)
		maxValue = value;
	    System.out.println("key:" + key + ", value:" + value);
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

    public List<PieChartModel> initPieComparative(String label, String label1, HashMap<String, Long> data, HashMap<String, Long> data2)
    {
	System.out.println("initPieModelComparative");
	PieChartModel p1 = new PieChartModel();
	PieChartModel p2 = new PieChartModel();

	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    p1.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	for(String key : data2.keySet())
	{
	    Long value = data2.get(key);
	    p2.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	p1.setTitle(label);
	p1.setLegendPosition("e");
	p1.setFill(false);
	p1.setShowDataLabels(true);
	p1.setDiameter(200);

	p2.setTitle(label1);
	p2.setLegendPosition("e");
	p2.setFill(false);
	p2.setShowDataLabels(true);
	p2.setDiameter(200);

	List<PieChartModel> listPie = new ArrayList<PieChartModel>();
	listPie.add(p1);
	listPie.add(p2);
	return listPie;

    }

    public void removeFakeUsers(List<User> listU)
    {
	//I should improve the class Sql to automatize this process (talk with Philipp)
	List<Integer> listUsers = new ArrayList<Integer>(Arrays.asList(new Integer[] { 1515, 1518, 1519, 1525, 1570, 1682, 2407, 2535, 2537, 2569, 2599, 2763, 2833, 2969, 3007, 3679, 5407, 5229, 5143, 7662, 7727, 8827, 8963, 8975, 9051, 9108 }));
	List<User> removeList = new ArrayList<User>();

	for(User u : listU)
	{
	    if(listUsers.contains(u.getId()))
	    {
		System.out.println(u.getUsername());
		removeList.add(u);
	    }
	}

	for(User u1 : removeList)
	    if(listU.contains(u1))
		listU.remove(u1);

	this.setUsers(listU);
    }

    public void selectClasses()
    {
	if(getUser() == null)
	    return;
	try
	{
	    courses = getUser().getCourses();
	    System.out.println(courses.toString());
	}
	catch(SQLException e1)
	{
	    addGrowl(FacesMessage.SEVERITY_FATAL, "Fatal error. Log out please.");
	    e1.printStackTrace();
	}
    }

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

    @Override
    public String toString()
    {
	return "UserAssessmentBean";
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
    }

    public String getSelectedGroup2()
    {
	return selectedGroup2;
    }

    public void setSelectedGroup2(String selectedGroup2)
    {
	this.selectedGroup2 = selectedGroup2;
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

}
