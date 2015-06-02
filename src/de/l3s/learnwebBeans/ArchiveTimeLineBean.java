package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.primefaces.context.RequestContext;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.UtilBean;

@SuppressWarnings("unchecked")
@ManagedBean
public class ArchiveTimeLineBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -991280404434096581L;

    public String getResourceJsonData() throws SQLException
    {

	JSONArray outerArray = new JSONArray();

	PreparedStatement select = Learnweb.getInstance().getConnection().prepareStatement("SELECT timestamp,count(*) as count FROM `lw_resource_archiveurl` WHERE `resource_id` = 110855 group by year(timestamp),month(timestamp) ORDER BY timestamp ASC");

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
	//RequestContext.getCurrentInstance().addCallbackParam("timelineData", outerArray.toJSONString());
	return outerArray.toJSONString();

    }

    public void getJsonData() throws SQLException
    {
	JSONArray outerArray = new JSONArray();
	int resourceId = getParameterInt("resource_id");
	PreparedStatement select = Learnweb.getInstance().getConnection().prepareStatement("SELECT timestamp,count(*) as count FROM `lw_resource_archiveurl` WHERE `resource_id` = ? group by year(timestamp),month(timestamp) ORDER BY timestamp ASC");
	select.setInt(1, resourceId);
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
	RequestContext.getCurrentInstance().addCallbackParam("timelineData", outerArray.toJSONString());
	//return outerArray.toJSONString();
    }

    public String getArchiveVersions(long unixTimestamp) throws SQLException
    {
	JSONObject archiveDates = new JSONObject();

	PreparedStatement select = Learnweb
		.getInstance()
		.getConnection()
		.prepareStatement(
			"SELECT DATE_FORMAT(t1.timestamp,'%Y-%m-%d') as day, COUNT(*) as no_of_versions FROM (SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=110852 AND MONTH(timestamp) = MONTH(FROM_UNIXTIME(" + unixTimestamp + "))) t1 GROUP BY DAY(t1.timestamp)");

	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    JSONObject archiveDay = new JSONObject();
	    archiveDay.put("number", rs.getInt("no_of_versions"));
	    archiveDay.put("badgeClass", "badge-warning");
	    select = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=75336 AND DAY(timestamp) = DAY('" + rs.getString("day") + "')");
	    ResultSet rsArchiveVersion = select.executeQuery();
	    JSONArray archiveVersions = new JSONArray();
	    while(rsArchiveVersion.next())
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
