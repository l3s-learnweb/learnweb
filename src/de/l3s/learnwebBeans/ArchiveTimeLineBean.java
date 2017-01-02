package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.primefaces.context.RequestContext;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.UtilBean;

@SuppressWarnings("unchecked")
@ManagedBean
@ViewScoped
public class ArchiveTimeLineBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -991280404434096581L;
    private final static Logger log = Logger.getLogger(ArchiveTimeLineBean.class);

    private int resourceId = 0;

    public void getResourceJsonData() throws SQLException
    {
        JSONArray outerArray = new JSONArray();

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
        log.debug(outerArray.toJSONString());
        RequestContext.getCurrentInstance().addCallbackParam("timelineData", outerArray.toJSONString());

    }

    public String getHighChartsJsonData()
    {
        JSONArray outerArray = new JSONArray();
        //int resourceId = getParameterInt("resource_id");
        try
        {
            PreparedStatement select = Learnweb.getInstance().getConnection().prepareStatement("SELECT timestamp,count(*) as count FROM `lw_resource_archiveurl` WHERE `resource_id` = ? group by year(timestamp),month(timestamp) ORDER BY timestamp ASC");
            select.setInt(1, resourceId);
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                JSONArray innerArray = new JSONArray();
                innerArray.add(rs.getTimestamp("timestamp").getTime());
                innerArray.add(rs.getInt("count"));
                outerArray.add(innerArray);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching the archive urls as json data for a resource", e);
            addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
        }
        log.debug(outerArray.toJSONString());
        RequestContext.getCurrentInstance().addCallbackParam("timelineData", outerArray.toJSONString());
        return outerArray.toJSONString();
    }

    public String getCalendarJsonData() throws SQLException
    {
        JSONObject archiveDates = new JSONObject();
        //int resourceId = getParameterInt("resource_id");
        PreparedStatement select = Learnweb.getInstance().getConnection()
                .prepareStatement("SELECT DATE_FORMAT(t1.timestamp,'%Y-%m-%d') as day, COUNT(*) as no_of_versions FROM (SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=?) t1 GROUP BY YEAR(t1.timestamp),MONTH(t1.timestamp),DAY(t1.timestamp) ORDER BY t1.timestamp");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            JSONObject archiveDay = new JSONObject();
            archiveDay.put("number", rs.getInt("no_of_versions"));
            archiveDay.put("badgeClass", "badge-warning");
            select = Learnweb.getInstance().getConnection()
                    .prepareStatement("SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=? AND YEAR(timestamp)=YEAR('" + rs.getString("day") + "') AND MONTH(timestamp)=MONTH('" + rs.getString("day") + "') AND DAY(timestamp) = DAY('" + rs.getString("day") + "')");
            select.setInt(1, resourceId);
            ResultSet rsArchiveVersion = select.executeQuery();
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
        log.debug(archiveDates.toJSONString());
        RequestContext.getCurrentInstance().addCallbackParam("calendarData", archiveDates.toJSONString());
        return archiveDates.toJSONString();

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
                .prepareStatement("SELECT DATE_FORMAT(t1.timestamp,'%Y-%m-%d') as day, COUNT(*) as no_of_versions FROM (SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=110805) t1 GROUP BY YEAR(t1.timestamp),MONTH(t1.timestamp),DAY(t1.timestamp) ORDER BY t1.timestamp");

        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            JSONObject archiveDay = new JSONObject();
            archiveDay.put("number", rs.getInt("no_of_versions"));
            archiveDay.put("badgeClass", "badge-warning");
            PreparedStatement select2 = Learnweb.getInstance().getConnection()
                    .prepareStatement("SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=110805 AND YEAR(timestamp)=YEAR('" + rs.getString("day") + "') AND MONTH(timestamp)=MONTH('" + rs.getString("day") + "') AND DAY(timestamp) = DAY('" + rs.getString("day") + "')");
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
        log.debug(archiveDates.toJSONString());
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
        log.debug(resourceId);
    }

}
