package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

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

@ManagedBean
@RequestScoped
public class UserAssessmentBean extends ApplicationBean implements Serializable
{
    /**
     * User assessment Bean
     * 
     * @author Alana Morais
     */

    private static final long serialVersionUID = 1L;
    private List<User> group1 = new ArrayList<User>();
    private List<User> group2 = new ArrayList<User>();
    private List<User> group3 = new ArrayList<User>();
    private List<Course> courses = new ArrayList<Course>();
    private List<User> users = new ArrayList<User>();

    private String selectCourse1;
    private String selectCourse2;
    private String selectCourse3;
    private String selectUser = "0";
    private String selectChart1;
    private String selectChart2;
    private String selectChart3;

    private BarChartModel barModel = new BarChartModel();
    private PieChartModel pieModel = new PieChartModel();
    private LineChartModel lineModel = new LineChartModel();

    private LineChartModel lineModel2 = new LineChartModel();

    public UserAssessmentBean()
    {
	identifyGroups();
	selectClasses();
	initBarModelComparative();
    }

    @PostConstruct
    public void init()
    {
	initBarModel();
	//initBarModelComparative();
    }

    public void identifyGroups()
    {
	//Kmeans algorithm
    }

    public void changeCourse()
    {
	String id = selectCourse3.substring(11, 14);
	int idCourse = Integer.valueOf(id);

	try
	{
	    UserManager usm = new UserManager(getLearnweb());
	    this.setUsers(usm.getUsersByCourseId(idCourse));
	    //Falta remover os admins e moderadores
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	}
    }

    public void initBarModelComparative()
    {
	lineModel2 = initCategoryModel();
	lineModel2.setTitle("Category Chart");
	lineModel2.setLegendPosition("e");
	lineModel2.setShowPointLabels(true);
	lineModel2.getAxes().put(AxisType.X, new CategoryAxis("Activities"));
	Axis yAxis = lineModel2.getAxis(AxisType.Y);
	yAxis.setLabel("Quantity");
	yAxis.setMin(0);
	yAxis.setMax(1000);
    }

    private LineChartModel initCategoryModel()
    {
	LineChartModel model = new LineChartModel();

	ChartSeries class1 = new ChartSeries();
	class1.setLabel("Harvey D and E");
	class1.set("Resources", 339);
	class1.set("Comments", 33);
	class1.set("Foruns Topics", 5);
	class1.set("Foruns Post", 18);

	ChartSeries class2 = new ChartSeries();
	class2.setLabel("Golgi A, B and C");
	class2.set("Resources", 909);
	class2.set("Comments", 196);
	class2.set("Foruns Topics", 21);
	class2.set("Foruns Post", 227);

	model.addSeries(class1);
	model.addSeries(class2);

	return model;
    }

    public void initBarModel()
    {
	// arrumar as info com dados reais
	ChartSeries group1 = new ChartSeries();
	group1.setLabel("User Activities");
	group1.set("Downloading", 40);
	group1.set("Searching", 40);
	group1.set("Rating Resource", 6);
	group1.set("Commenting Resource", 10);
	group1.set("Tagging Resource", 5);

	barModel.addSeries(group1);
	barModel.setTitle("Bar Chart");

	Axis xAxis = barModel.getAxis(AxisType.X);
	xAxis.setLabel("Activity Type");

	Axis yAxis = barModel.getAxis(AxisType.Y);
	yAxis.setLabel("Quantity");
	yAxis.setMin(0);
	yAxis.setMax(50);
    }

    public void initPieModel()
    {
	pieModel = new PieChartModel();

	pieModel.set("Downloading", 40);
	pieModel.set("Searching", 40);
	pieModel.set("Rating Resource", 6);
	pieModel.set("Commenting Resource", 10);
	pieModel.set("Tagging Resource", 5);

	pieModel.setTitle("Pie Chart");
	pieModel.setLegendPosition("ne");
	pieModel.setFill(false);
	pieModel.setShowDataLabels(true);
	pieModel.setDiameter(250);
    }

    public void generateGraph()
    {
	if(this.selectCourse3 == null)
	    return;
    }

    public void selectUsers()
    {
	if(this.selectCourse3 == null)
	    return;
	else
	{
	    if(this.selectCourse3 == "bar")
	    {

	    }
	    else if(this.selectCourse3 == "pie")
	    {

	    }
	    else if(this.selectChart3 == "line")
	    {

	    }

	}

    }

    public void selectClasses()
    {
	if(getUser() == null)
	    return;
	try
	{
	    courses = getUser().getCourses();
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

    public String getSelectCourse2()
    {
	return selectCourse2;
    }

    public void setSelectCourse2(String selectCourse2)
    {
	this.selectCourse2 = selectCourse2;
    }

    public String getSelectUser()
    {
	return selectUser;
    }

    public void setSelectUser(String selectUser)
    {
	this.selectUser = selectUser;
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

    public LineChartModel getLineModel()
    {
	return lineModel;
    }

    public void setLineModel(LineChartModel lineModel)
    {
	this.lineModel = lineModel;
    }
}
