package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.faces.view.ViewScoped;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.primefaces.PrimeFaces;

import de.l3s.learnweb.Learnweb;

@SuppressWarnings("unchecked")
@Named
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
        // TODO Dupe: the same code occurred 4 times in the class
        while(rs.next())
        {
            JSONArray innerArray = new JSONArray();
            //series1.set(rs.getString("date"), rs.getInt("count"));
            innerArray.put(rs.getTimestamp("timestamp").getTime());
            innerArray.put(rs.getInt("count"));
            outerArray.put(innerArray);
        }
        //outerArray.add(innerArray);
        log.debug(outerArray.toString());
        PrimeFaces.current().ajax().addCallbackParam("timelineData", outerArray.toString());

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
                innerArray.put(rs.getTimestamp("timestamp").getTime());
                innerArray.put(rs.getInt("count"));
                outerArray.put(innerArray);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching the archive urls as json data for a resource", e);
            addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
        }
        log.debug(outerArray.toString());
        PrimeFaces.current().ajax().addCallbackParam("timelineData", outerArray.toString());
        return outerArray.toString();
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
                archiveVersions.put(archiveVersion);
            }
            archiveDay.put("dayEvents", archiveVersions);
            archiveDates.put(rs.getString("day"), archiveDay);
        }
        log.debug(archiveDates.toString());
        PrimeFaces.current().ajax().addCallbackParam("calendarData", archiveDates.toString());
        return archiveDates.toString();

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
        for (String month : symbols.getMonths()) {
            if (!month.isBlank()) {
                monthNames.put(month);
            }
        }
        return monthNames.toString();
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
                archiveVersions.put(archiveVersion);
            }
            archiveDay.put("dayEvents", archiveVersions);
            archiveDates.put(rs.getString("day"), archiveDay);
        }
        log.debug(archiveDates.toString());
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
