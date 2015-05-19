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

import org.apache.log4j.Logger;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.PieChartModel;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.User;
import de.l3s.learnweb.UserManager;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.util.Sql;

/**
 * User Assessment Bean
 * 
 * @author Alana Morais
 */
@ManagedBean
@SessionScoped
public class UserAssessmentBean extends ApplicationBean implements Serializable
{
    private final static long serialVersionUID = 3991177737812918816L;
    private final static Logger log = Logger.getLogger(UserAssessmentBean.class);

    private List<User> group1 = new ArrayList<User>();
    private List<User> group2 = new ArrayList<User>();
    private List<User> group3 = new ArrayList<User>();
    private List<Course> courses = new ArrayList<Course>();
    private List<User> users = new ArrayList<User>();

    private Map<String, String> infoDetailUser = new HashMap<String, String>();

    private String selectCourse1;
    private String selectCourse20; // TODO please use better names
    private String selectCourse21;
    private String selectCourse3;
    private String selectedUser;
    private String selectChart1;
    private String selectChart2;
    private String selectChart3;

    private boolean showb3, showl3, showp3 = false;
    private boolean showb2, showl2, showp2 = false;
    private BarChartModel barModel = new BarChartModel();
    private BarChartModel barModel2 = new BarChartModel();
    private PieChartModel pieModel = new PieChartModel();
    private PieChartModel pieModel20 = new PieChartModel(); // TODO please use better names
    private PieChartModel pieModel21 = new PieChartModel();
    private LineChartModel lineModel = new LineChartModel();
    private LineChartModel lineModel2 = new LineChartModel();

    public UserAssessmentBean()
    {
	this.selectCourse1 = null;
	this.selectCourse20 = null;
	this.selectCourse21 = null;
	this.selectCourse3 = null;
	this.selectedUser = null;
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

    public HashMap<String, Long> searchGeneralActivities(String classe)
    {
	//General Activities in each class
	// SELECT * FROM `lw_user_log` A1 INNER JOIN `lw_user_course` A2 ON A1.user_id = A2.user_id WHERE A2.course_id=640 AND A1.action=X
	HashMap<String, Long> listGeneralActivities = new HashMap<String, Long>();
	try
	{
	    Long ratting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=1");
	    Long tagging = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=0");
	    Long commenting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=2");
	    Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=5");
	    Long downloading = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=32");
	    Long editResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + classe + " AND A1.action=19");

	    /**
	     * I guess one query will be faster than 6.
	     * 
	     * WHERE A2.course_id=" + classe + " AND A1.action=19
	     * 
	     * Adding a parameter to the query is a security vulnerability.
	     * PrepearedStatements are more secure.
	     * 
	     * PreparedStatement select = getLearnweb().getConnection().prepareStatement(
	     * "SELECT action, count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 USING(user_id) WHERE A2.course_id=? GROUP BY A1.action");
	     * select.setString(1, classe);
	     * ResultSet rs = select.executeQuery();
	     * while(rs.next())
	     * {
	     * int action = rs.getInt(1);
	     * int count = rs.getInt(2);
	     * 
	     * if(action == 19)
	     * listGeneralActivities.put("Edit Resource", (long) count);
	     * }
	     * rs.close();
	     */
	    listGeneralActivities.put("Ratting Resources", ratting);
	    listGeneralActivities.put("Tagging Resources", tagging);
	    listGeneralActivities.put("Comments", commenting);
	    listGeneralActivities.put("Searching", searching);
	    listGeneralActivities.put("Downloading", downloading);
	    listGeneralActivities.put("Edit Resource", editResource);
	    return listGeneralActivities;
	}
	catch(SQLException e)
	{
	    log.error("Couldn't genereta statistics", e); // TODO use log.info/debug/error instead of system.println
	    return null;
	}
    }

    public HashMap<String, Long> searchActivities()
    {
	// SELECT * FROM `lw_user_log` A1 INNER JOIN `lw_user_course` A2 ON A1.user_id = A2.user_id WHERE A2.course_id =640 AND A1.user_id =7727 
	HashMap<String, Long> listActivities = new HashMap<String, Long>();
	try
	{
	    Long ratting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=1");
	    Long tagging = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=0");
	    Long commenting = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=2");
	    Long searching = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=5");
	    Long downloading = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=32");
	    Long editResource = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user_log A1 INNER JOIN lw_user_course A2 ON A1.user_id = A2.user_id WHERE A2.course_id=" + this.selectCourse3 + " AND A1.user_id =" + this.selectedUser + " AND A1.action=19");

	    listActivities.put("Ratting Resources", ratting);
	    listActivities.put("Tagging Resources", tagging);
	    listActivities.put("Comments", commenting);
	    listActivities.put("Searching", searching);
	    listActivities.put("Downloading", downloading);
	    listActivities.put("Edit Resource", editResource);

	    System.out.println("Ratting Resource" + ratting);
	    System.out.println("Tagging Resources" + tagging);
	    System.out.println("Comments" + commenting);
	    System.out.println("Searching" + searching);

	    //GraphGenerator gg = new GraphGenerator("User Details", "", "User Activities", "", "Activities", "", "Values", "", "bar", listActivities, null);
	    //this.barModel = gg.getBarModel();
	    return listActivities;

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
	System.out.println(this.selectChart3);

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
	    System.out.println(list);
	    this.initBarModelUser("User Details:" + this.selectedUser, "User Activities", list, "Activities", "Number of Interactions");

	    System.out.println("Aqui if B");
	}

	if((this.selectChart3.compareTo("line") == 0) && (this.showl3 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    list = this.searchActivities();
	    System.out.println(list);
	    this.initLineModelUser("User Details:" + this.selectedUser, "User Activities", list, "Activities", "Number of Interactions");
	}

	if((this.selectChart3.compareTo("pie") == 0) && (this.showp3 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    list = this.searchActivities();
	    System.out.println(list);
	    this.initPieModelUser("User Details:" + this.selectedUser, "User Activities", list);
	}
    }

    public void generateComparativeGraph()
    {
	System.out.println("generateComparativeGraph");
	System.out.println(this.selectChart2);

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
	    list = this.searchGeneralActivities(this.selectCourse20);
	    list2 = this.searchGeneralActivities(this.selectCourse21);
	    System.out.println(list);
	    System.out.println(list2);
	    this.initBarComparative("Comparison between " + this.selectCourse20 + " and " + this.selectCourse21 + " class", this.selectCourse20, this.selectCourse21, list, list2, "Activities", "Number of Interactions");
	}

	if((this.selectChart2.compareTo("line") == 0) && (this.showl2 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    HashMap<String, Long> list2 = new HashMap<String, Long>();
	    list = this.searchGeneralActivities(this.selectCourse20);
	    list2 = this.searchGeneralActivities(this.selectCourse21);
	    System.out.println(list);
	    System.out.println(list2);
	    this.initLineComparative("Comparison between " + this.selectCourse20 + " and " + this.selectCourse21 + " class", this.selectCourse20, this.selectCourse21, list, list2, "Activities", "Number of Interactions");
	}

	if((this.selectChart2.compareTo("pie") == 0) && (this.showp2 == true))
	{
	    HashMap<String, Long> list = new HashMap<String, Long>();
	    HashMap<String, Long> list2 = new HashMap<String, Long>();
	    list = this.searchGeneralActivities(this.selectCourse20);
	    list2 = this.searchGeneralActivities(this.selectCourse21);
	    System.out.println(list);
	    System.out.println(list2);
	    this.initPieComparative(this.selectCourse20, this.selectCourse21, list, list2);
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

	ChartSeries class1 = new ChartSeries();
	class1.setLabel(label);
	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    class1.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	this.lineModel.addSeries(class1);
	this.lineModel.setTitle(text);
	this.lineModel.setLegendPosition("e");
	this.lineModel.setShowPointLabels(true);
	this.lineModel.getAxes().put(AxisType.X, new CategoryAxis(xLabel));
	Axis yAxis = this.lineModel.getAxis(AxisType.Y);
	yAxis.setLabel(yLabel);
	yAxis.setMin(0);
	yAxis.setMax(50);
    }

    public void initBarModelUser(String text, String label, HashMap<String, Long> data, String xLabel, String yLabel)
    {
	System.out.println("initBarModelUser");
	this.barModel = new BarChartModel();

	this.barModel.setTitle(text);
	this.barModel.setLegendPosition("ne");

	ChartSeries group1 = new ChartSeries();
	group1.setLabel(label);

	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    group1.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	this.barModel.addSeries(group1);
	Axis xAxis = this.barModel.getAxis(AxisType.X);
	xAxis.setLabel(xLabel);
	Axis yAxis = this.barModel.getAxis(AxisType.Y);
	yAxis.setLabel(yLabel);
	yAxis.setMin(0);
	yAxis.setMax(50);

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
	this.pieModel.setLegendPosition("e");
	this.pieModel.setFill(false);
	this.pieModel.setShowDataLabels(true);
	this.pieModel.setDiameter(200);
    }

    public void initBarComparative(String text, String label, String label1, HashMap<String, Long> data, HashMap<String, Long> data2, String xLabel, String yLabel)
    {
	System.out.println("initBarComparative");
	this.barModel2 = new BarChartModel();

	this.barModel2.setTitle(text);
	this.barModel2.setLegendPosition("ne");

	ChartSeries group1 = new ChartSeries();
	group1.setLabel("id " + label);

	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    group1.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	ChartSeries group2 = new ChartSeries();
	group2.setLabel("id " + label1);

	for(String key2 : data2.keySet())
	{
	    Long value = data2.get(key2);
	    group2.set(key2, value);
	    System.out.println("key:" + key2 + ", value:" + value);
	}

	this.barModel2.addSeries(group1);
	this.barModel2.addSeries(group2);
	Axis xAxis = this.barModel2.getAxis(AxisType.X);
	xAxis.setLabel(xLabel);
	Axis yAxis = this.barModel2.getAxis(AxisType.Y);
	yAxis.setLabel(yLabel);
	yAxis.setMin(0);
	yAxis.setMax(1200);

    }

    public void initLineComparative(String text, String label, String label1, HashMap<String, Long> data, HashMap<String, Long> data2, String xLabel, String yLabel)
    {
	System.out.println("initLineComprative");
	this.lineModel2 = new LineChartModel();

	ChartSeries class1 = new ChartSeries();
	class1.setLabel("id " + label);
	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    class1.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	ChartSeries class2 = new ChartSeries();
	class2.setLabel("id " + label1);
	for(String key : data2.keySet())
	{
	    Long value = data2.get(key);
	    class2.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	this.lineModel2.addSeries(class1);
	this.lineModel2.addSeries(class2);
	this.lineModel2.setTitle(text);
	this.lineModel2.setLegendPosition("e");
	this.lineModel2.setShowPointLabels(true);
	this.lineModel2.getAxes().put(AxisType.X, new CategoryAxis(xLabel));
	Axis yAxis = this.lineModel2.getAxis(AxisType.Y);
	yAxis.setLabel(yLabel);
	yAxis.setMin(0);
	yAxis.setMax(1200);
    }

    public void initPieComparative(String label, String label1, HashMap<String, Long> data, HashMap<String, Long> data2)
    {
	System.out.println("initLineModelComparative");
	this.pieModel20 = new PieChartModel();
	this.pieModel21 = new PieChartModel();

	for(String key : data.keySet())
	{
	    Long value = data.get(key);
	    this.pieModel20.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	for(String key : data2.keySet())
	{
	    Long value = data2.get(key);
	    this.pieModel21.set(key, value);
	    System.out.println("key:" + key + ", value:" + value);
	}

	this.pieModel20.setTitle(label);
	this.pieModel20.setLegendPosition("e");
	this.pieModel20.setFill(false);
	this.pieModel20.setShowDataLabels(true);
	this.pieModel20.setDiameter(200);

	this.pieModel21.setTitle(label1);
	this.pieModel21.setLegendPosition("e");
	this.pieModel21.setFill(false);
	this.pieModel21.setShowDataLabels(true);
	this.pieModel21.setDiameter(200);

    }

    public void removeFakeUsers(List<User> listU)
    {
	List<Integer> listUsers = new ArrayList<Integer>(Arrays.asList(new Integer[] { 2282, 2298, 2300, 2302, 2310, 2401, 2405, 2507, 2509, 2539, 2541, 2761, 2857, 3045, 3417, 3439, 3673, 5215, 5217, 7205, 7208, 7210, 7213, 7254, 7252, 7292, 7321, 7381, 7448, 7449, 7512, 7822,
		8110, 8111, 8306, 8875, 8876, 8881, 8910, 8920, 8921, 8925, 8964, 9069, 1656, 1674, 1676, 1678, 1680, 1682, 2292, 5287, 2225, 5289, 7207, 7209, 5305, 5407, 7727, 2969, 5143, 7662, 5229 }));
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

}
