package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TimelineManager
{
    //private final static Logger log = Logger.getLogger(TimelineManager.class);
    private final Learnweb learnweb;

    protected TimelineManager(Learnweb learnweb)
    {
	this.learnweb = learnweb;
    }

    public List<TimelineData> getTimelineDataGroupedByMonth(int resourceId) throws SQLException
    {
	List<TimelineData> timelineMonthlyData = new LinkedList<TimelineData>();
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT timestamp,count(*) as no_of_versions FROM `lw_resource_archiveurl` WHERE `resource_id` = ? group by year(timestamp),month(timestamp) ORDER BY timestamp ASC");
	select.setInt(1, resourceId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    TimelineData timelineData = new TimelineData(rs.getTimestamp("timestamp"), rs.getInt("no_of_versions"));
	    timelineMonthlyData.add(timelineData);
	}
	return timelineMonthlyData;
    }

    public List<TimelineData> getTimelineDataGroupedByDay(int resourceId) throws SQLException
    {
	List<TimelineData> timelineDailyData = new LinkedList<TimelineData>();
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT timestamp, COUNT(*) as no_of_versions FROM lw_resource_archiveurl WHERE `resource_id`=? GROUP BY YEAR(timestamp),MONTH(timestamp),DAY(timestamp) ORDER BY timestamp");
	select.setInt(1, resourceId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    TimelineData timelineData = new TimelineData(rs.getTimestamp("timestamp"), rs.getInt("no_of_versions"));
	    timelineDailyData.add(timelineData);
	}
	return timelineDailyData;
    }

    public List<ArchiveUrl> getArchiveUrlsByResourceIdAndTimestamp(int resourceId, Date timestamp) throws SQLException
    {
	List<ArchiveUrl> archiveUrlsData = new LinkedList<ArchiveUrl>();
	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_resource_archiveurl WHERE `resource_id`=? AND DATE(timestamp)= DATE(?)");
	select.setInt(1, resourceId);
	select.setTimestamp(2, new java.sql.Timestamp(timestamp.getTime()));
	ResultSet rs = select.executeQuery();
	while(rs.next())
	{
	    ArchiveUrl archiveUrl = new ArchiveUrl(rs.getString("archive_url"), rs.getTimestamp("timestamp"));
	    archiveUrlsData.add(archiveUrl);
	}
	return archiveUrlsData;
    }
}
