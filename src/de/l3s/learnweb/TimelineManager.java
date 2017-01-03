package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class TimelineManager
{
    //private final static Logger log = Logger.getLogger(TimelineManager.class);
    private final Learnweb learnweb;

    protected TimelineManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public List<TimelineData> getTimelineDataGroupedByMonth(int resourceId, String url) throws SQLException
    {
        List<TimelineData> timelineMonthlyData = new LinkedList<TimelineData>();
        TreeMap<Date, Integer> monthlySeriesData = new TreeMap<Date, Integer>();
        PreparedStatement pStmt1 = learnweb.getConnection().prepareStatement("SELECT timestamp,count(*) as no_of_versions FROM `lw_resource_archiveurl` WHERE `resource_id` = ? group by year(timestamp),month(timestamp) ORDER BY timestamp ASC");
        pStmt1.setInt(1, resourceId);
        ResultSet rs = pStmt1.executeQuery();
        while(rs.next())
        {
            monthlySeriesData.put(getFirstDayOfMonth(rs.getTimestamp(1)), rs.getInt(2));
            //TimelineData timelineData = new TimelineData(getFirstDayOfMonth(rs.getTimestamp(1)), rs.getInt(2));
            //timelineMonthlyData.add(timelineData);
        }
        PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement("SELECT url_id FROM wb_url WHERE url = ?");
        pStmt2.setString(1, url);
        ResultSet rs2 = pStmt2.executeQuery();
        if(rs2.next())
        {
            PreparedStatement pStmt3 = learnweb.getConnection().prepareStatement("SELECT timestamp,count(*) FROM `wb_url_capture` WHERE `url_id` = ? group by year(timestamp),month(timestamp) ORDER BY timestamp ASC");
            pStmt3.setInt(1, rs2.getInt(1));
            ResultSet rs3 = pStmt3.executeQuery();
            while(rs3.next())
            {
                Date timestamp = getFirstDayOfMonth(rs3.getTimestamp(1));
                if(monthlySeriesData.containsKey(timestamp))
                    monthlySeriesData.put(timestamp, monthlySeriesData.get(timestamp) + rs3.getInt(2));
                else
                    monthlySeriesData.put(timestamp, rs3.getInt(2));
            }
        }
        for(Date timestamp : monthlySeriesData.keySet())
        {
            TimelineData timelineData = new TimelineData(timestamp, monthlySeriesData.get(timestamp));
            timelineMonthlyData.add(timelineData);
        }
        return timelineMonthlyData;
    }

    public List<TimelineData> getTimelineDataGroupedByDay(int resourceId, String url) throws SQLException
    {
        List<TimelineData> timelineDailyData = new LinkedList<TimelineData>();
        TreeMap<Date, Integer> dailySeriesData = new TreeMap<Date, Integer>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT timestamp, COUNT(*) as no_of_versions FROM lw_resource_archiveurl WHERE `resource_id`=? GROUP BY YEAR(timestamp),MONTH(timestamp),DAY(timestamp) ORDER BY timestamp");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            dailySeriesData.put(getMidDayOfDate(rs.getTimestamp(1)), rs.getInt(2));
            //TimelineData timelineData = new TimelineData(getMidDayOfDate(rs.getTimestamp(1)), rs.getInt(2));
            //timelineDailyData.add(timelineData);
        }
        PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement("SELECT url_id FROM wb_url WHERE url = ?");
        pStmt2.setString(1, url);
        ResultSet rs2 = pStmt2.executeQuery();
        if(rs2.next())
        {
            PreparedStatement pStmt3 = learnweb.getConnection().prepareStatement("SELECT timestamp,count(*) FROM `wb_url_capture` WHERE `url_id` = ? group by year(timestamp),month(timestamp),day(timestamp) ORDER BY timestamp ASC");
            pStmt3.setInt(1, rs2.getInt(1));
            ResultSet rs3 = pStmt3.executeQuery();
            while(rs3.next())
            {
                Date timestamp = getMidDayOfDate(rs3.getTimestamp(1));
                if(dailySeriesData.containsKey(timestamp))
                    dailySeriesData.put(timestamp, dailySeriesData.get(timestamp) + rs3.getInt(2));
                else
                    dailySeriesData.put(timestamp, rs3.getInt(2));
            }
        }
        for(Date timestamp : dailySeriesData.keySet())
        {
            TimelineData timelineData = new TimelineData(timestamp, dailySeriesData.get(timestamp));
            timelineDailyData.add(timelineData);
        }

        return timelineDailyData;
    }

    public List<ArchiveUrl> getArchiveUrlsByResourceIdAndTimestamp(int resourceId, Date timestamp, String url) throws SQLException
    {
        SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
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
        PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement("SELECT url_id FROM wb_url WHERE url = ?");
        pStmt2.setString(1, url);
        ResultSet rs2 = pStmt2.executeQuery();
        if(rs2.next())
        {
            PreparedStatement pStmt3 = learnweb.getConnection().prepareStatement("SELECT timestamp FROM `wb_url_capture` WHERE `url_id` = ? AND DATE(timestamp) = DATE(?)");
            pStmt3.setInt(1, rs2.getInt(1));
            pStmt3.setTimestamp(2, new Timestamp(timestamp.getTime()));
            ResultSet rs3 = pStmt3.executeQuery();
            while(rs3.next())
            {
                Date archiveTimestamp = rs3.getTimestamp(1);
                ArchiveUrl archiveUrl = new ArchiveUrl("https://web.archive.org/web/" + waybackDateFormat.format(archiveTimestamp) + "/" + url, archiveTimestamp);
                archiveUrlsData.add(archiveUrl);
            }
        }
        return archiveUrlsData;
    }

    private Date getFirstDayOfMonth(Date timestamp)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        return cal.getTime();
    }

    private Date getMidDayOfDate(Date timestamp)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        return cal.getTime();
    }
}
