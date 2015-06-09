package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.TimelineData;
import de.l3s.learnweb.beans.UtilBean;

@SuppressWarnings("unchecked")
@ManagedBean
@ViewScoped
public class ResourceDetailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4468979717844804599L;
    private final static Logger log = Logger.getLogger(ResourceDetailBean.class);
    private int resourceId = 0;

    public String getHighChartsJsonData()
    {
	JSONArray highChartsData = new JSONArray();
	try
	{
	    List<TimelineData> timelineMonthlyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByMonth(resourceId);

	    for(TimelineData timelineData : timelineMonthlyData)
	    {
		JSONArray innerArray = new JSONArray();
		innerArray.add(timelineData.getTimestamp().getTime());
		innerArray.add(timelineData.getNumberOfVersions());
		highChartsData.add(innerArray);
	    }
	}
	catch(SQLException e)
	{
	    log.error("Error while fetching the archive data aggregated by month for a resource", e);
	    addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
	}
	return highChartsData.toJSONString();
    }

    public String getCalendarJsonData()
    {
	JSONObject archiveDates = new JSONObject();
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	try
	{
	    List<TimelineData> timelineDailyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByDay(resourceId);
	    for(TimelineData timelineData : timelineDailyData)
	    {
		JSONObject archiveDay = new JSONObject();
		archiveDay.put("number", timelineData.getNumberOfVersions());
		archiveDay.put("badgeClass", "badge-warning");
		List<ArchiveUrl> archiveUrlsData = getLearnweb().getTimelineManager().getArchiveUrlsByResourceIdAndTimestamp(resourceId, timelineData.getTimestamp());
		JSONArray archiveVersions = new JSONArray();
		for(ArchiveUrl archiveUrl : archiveUrlsData)
		{
		    JSONObject archiveVersion = new JSONObject();
		    archiveVersion.put("url", archiveUrl.getArchiveUrl());
		    archiveVersion.put("time", DateFormat.getTimeInstance(DateFormat.MEDIUM, UtilBean.getUserBean().getLocale()).format(archiveUrl.getTimestamp()));
		    archiveVersions.add(archiveVersion);
		}
		archiveDay.put("dayEvents", archiveVersions);
		archiveDates.put(dateFormat.format(timelineData.getTimestamp()), archiveDay);
	    }
	}
	catch(SQLException e)
	{
	    log.error("Error while fetching the archive data aggregated by day for a resource", e);
	    addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
	}
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

    public String getShortMonthNames()
    {
	DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
	JSONArray monthNames = new JSONArray();
	for(String month : symbols.getShortMonths())
	{
	    monthNames.add(month);
	}
	monthNames.remove(""); //To remove empty string from the array
	return monthNames.toJSONString();
    }

    public int getResourceId()
    {
	return resourceId;
    }

    public void setResourceId(int resourceId)
    {
	this.resourceId = resourceId;
    }
}
