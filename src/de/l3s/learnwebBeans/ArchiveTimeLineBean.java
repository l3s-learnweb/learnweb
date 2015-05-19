package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.primefaces.event.ItemSelectEvent;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.DateAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.PieChartModel;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
public class ArchiveTimeLineBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -991280404434096581L;
    private LineChartModel dateModel;
    private PieChartModel pieModel1;
    private Resource testResource;

    @PostConstruct
    public void init()
    {
	try
	{
	    createDateModel();
	    createPieModels();
	}
	catch(SQLException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public LineChartModel getDateModel()
    {
	return dateModel;
    }

    public PieChartModel getPieModel1()
    {
	return pieModel1;
    }

    private void createDateModel() throws SQLException
    {
	testResource = Learnweb.getInstance().getResourceManager().getResource(75336);
	//List<ArchiveUrl> archiveUrls = testResource.getArchiveUrls();

	dateModel = new LineChartModel();
	LineChartSeries series1 = new LineChartSeries();
	series1.setLabel("Series 1");
	//model = new BarChartModel();
	//ChartSeries series = new ChartSeries();
	PreparedStatement select = Learnweb.getInstance().getConnection().prepareStatement("SELECT DATE_FORMAT(timestamp,'%Y-%m') as date, COUNT(archive_url) as count FROM `lw_resource_archiveurl` WHERE `resource_id` = 75336 GROUP BY month(timestamp)");
	//PreparedStatement select = Learnweb.getInstance().getConnection().prepareStatement("SELECT timestamp, COUNT(archive_url) as count FROM `lw_resource_archiveurl` WHERE `resource_id` = 75336");
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    series1.set(rs.getString("date"), rs.getInt("count"));

	}

	//	series1.set("2014-01-01", 51);
	//	series1.set("2014-01-06", 22);
	//	series1.set("2014-01-12", 65);
	//	series1.set("2014-01-18", 74);
	//	series1.set("2014-01-24", 24);
	//	series1.set("2014-01-30", 51);

	dateModel.addSeries(series1);

	dateModel.setTitle("Zoom for Details");
	dateModel.setZoom(true);
	dateModel.getAxis(AxisType.Y).setLabel("Number of Archived Versions");
	DateAxis axis = new DateAxis("Dates");
	axis.setTickAngle(-50);
	//axis.setMax("2014-02-01");
	axis.setTickFormat("%b, %y");
	dateModel.getAxes().put(AxisType.X, axis);
    }

    private void createPieModels()
    {
	createPieModel1();
    }

    private void createPieModel1()
    {
	pieModel1 = new PieChartModel();

	pieModel1.set("Brand 1", 540);
	pieModel1.set("Brand 2", 325);
	pieModel1.set("Brand 3", 702);
	pieModel1.set("Brand 4", 421);

	pieModel1.setTitle("Simple Pie");
	pieModel1.setLegendPosition("w");
    }

    public void itemSelect(ItemSelectEvent event)
    {
	System.out.println("Item Index: " + event.getItemIndex() + ", Series Index:" + event.getSeriesIndex());
	addGrowl(FacesMessage.SEVERITY_INFO, "Item selected", "Item Index: " + event.getItemIndex() + ", Series Index:" + event.getSeriesIndex());
    }

    public String getJsonData() throws SQLException
    {
	JSONArray outerArray = new JSONArray();

	PreparedStatement select = Learnweb.getInstance().getConnection().prepareStatement("SELECT timestamp,count(*) as count FROM `lw_resource_archiveurl` WHERE `resource_id` = 75336 group by month(timestamp) ORDER BY timestamp ASC");
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    JSONArray innerArray = new JSONArray();
	    //series1.set(rs.getString("date"), rs.getInt("count"));
	    innerArray.add(rs.getTimestamp("timestamp").getTime());
	    innerArray.add(rs.getInt("count"));
	    outerArray.add(innerArray);
	}
	//outerArray.add(innerArray);
	System.out.println(outerArray.toJSONString());
	return outerArray.toJSONString();
    }

    public String getArchiveVersions(long unixTimestamp) throws SQLException
    {
	JSONObject archiveDates = new JSONObject();

	PreparedStatement select = Learnweb
		.getInstance()
		.getConnection()
		.prepareStatement(
			"SELECT DATE_FORMAT(t1.timestamp,'%Y-%m-%d') as day, COUNT(*) as no_of_versions FROM (SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=75336 AND MONTH(timestamp) = MONTH(FROM_UNIXTIME(" + unixTimestamp + "))) t1 GROUP BY DAY(t1.timestamp)");

	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    JSONObject archiveDay = new JSONObject();
	    archiveDay.put("number", rs.getInt("no_of_versions"));
	    archiveDay.put("badgeClass", "badge-warning");
	    select = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=75336 AND DAY(timestamp) = DAY('" + rs.getString("day") + "')");
	    ResultSet rsArchiveVersion = select.executeQuery();
	    JSONArray archiveVersions = new JSONArray();
	    while(rs.next())
	    {
		JSONObject archiveVersion = new JSONObject();
		archiveVersion.put("url", rs.getString("archive_url"));
		archiveVersion.put("time", rs.getTimestamp("timestamp"));
		archiveVersions.add(archiveVersion);
	    }
	    archiveDates.put(rs.getString("day"), archiveDay);
	}
	System.out.println(archiveDates.toJSONString());
	return "";

    }

    //Function to get short week day names for the calendar
    public List<String> getShortWeekDays()
    {
	DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
	List<String> dayNames = Arrays.asList(symbols.getShortWeekdays());
	return dayNames.subList(1, 8);
    }

    //Function to localized month names for the calendar 
    public String getMonthNames()
    {
	DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
	JSONArray monthNames = new JSONArray();
	for(String month : symbols.getMonths())
	{
	    monthNames.add(month);
	}
	monthNames.remove(""); //To remove empty string from the array
	return monthNames.toJSONString();
    }

    public static void main(String[] args) throws SQLException
    {

	JSONObject archiveDates = new JSONObject();

	PreparedStatement select = Learnweb.getInstance().getConnection()
		.prepareStatement("SELECT DATE_FORMAT(t1.timestamp,'%Y-%m-%d') as day, COUNT(*) as no_of_versions FROM (SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=75336 AND MONTH(timestamp) = MONTH(FROM_UNIXTIME(1289838290))) t1 GROUP BY DAY(t1.timestamp)");

	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    JSONObject archiveDay = new JSONObject();
	    archiveDay.put("number", rs.getInt("no_of_versions"));
	    archiveDay.put("badgeClass", "badge-warning");
	    PreparedStatement select2 = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=75336 AND DAY(timestamp) = DAY('" + rs.getString("day") + "')");
	    ResultSet rsArchiveVersion = select2.executeQuery();
	    JSONArray archiveVersions = new JSONArray();
	    while(rsArchiveVersion.next())
	    {
		JSONObject archiveVersion = new JSONObject();
		archiveVersion.put("url", rsArchiveVersion.getString("archive_url"));
		archiveVersion.put("time", rsArchiveVersion.getTimestamp("timestamp").toString());
		archiveVersions.add(archiveVersion);
	    }
	    archiveDay.put("dayEvents", archiveVersions);
	    archiveDates.put(rs.getString("day"), archiveDay);
	}
	System.out.println(archiveDates.toJSONString());
    }
}
