package de.l3s.learnweb;

import org.apache.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AnnouncementsManager
{
    private final static Logger log = Logger.getLogger(AnnouncementsManager.class);

    private Learnweb learnweb;
    private Map<Integer, Announcement> cache;

    public AnnouncementsManager(Learnweb learnweb)
    {
        super();
        this.learnweb = learnweb;
        this.cache = new LinkedHashMap<>();
    }

    public void resetCache(int limit) throws SQLException
    {
        try(PreparedStatement preparedStatement = learnweb.getConnection().prepareStatement("SELECT * FROM lw_news WHERE hidden = ? ORDER BY created_at DESC limit ?"))
        {
            preparedStatement.setBoolean(1, false);
            preparedStatement.setInt(2, limit);
            ResultSet resultSet = preparedStatement.executeQuery();
            cache.clear();
            while(resultSet.next())
            {
                Announcement announcement = createAnnouncement(resultSet);
                cache.put(announcement.getId(), announcement);

            }
        }
    }

    private static Announcement createAnnouncement(ResultSet rs) throws SQLException
    {
        Announcement announcement = new Announcement();
        announcement.setId(rs.getInt("news_id"));
        announcement.setTitle(rs.getString("title"));
        announcement.setText(rs.getString("message"));
        announcement.setUserId(rs.getInt("user_id"));
        announcement.setDate(rs.getDate("created_at")); // TODO be careful with SQL getDate. It will really only return the date but not the time.
        announcement.setHidden(rs.getBoolean("hidden"));
        return announcement;
    }

    public Announcement save(Announcement announcement) throws SQLException
    {
        try(PreparedStatement stmt = learnweb.getConnection().prepareStatement("INSERT INTO lw_news (title, message, user_id, created_at, hidden) VALUES (?, ?, ?, ?, ?)"))
        {
            stmt.setString(1, announcement.getTitle());
            stmt.setString(2, announcement.getText());
            stmt.setInt(3, announcement.getUserId());
            stmt.setDate(4, sqlDate(announcement.getDate()));
            stmt.setBoolean(5, announcement.isHidden());
            log.debug(stmt.toString());
            stmt.executeUpdate();
        }
        return announcement;
    }

    public void delete(Announcement announcement) throws SQLException
    {
        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_news` WHERE news_id = ?"))
        {
            delete.setInt(1, announcement.getId());
            log.debug(delete.toString());
            delete.executeUpdate();
        }
        cache.remove(announcement.getId());
    }

    public void update(Announcement announcement) throws SQLException
    {
        try(PreparedStatement stmt = learnweb.getConnection().prepareStatement("UPDATE lw_news SET title = ?, message = ?, created_at = ?, hidden = ?  WHERE news_id = ?"))
        {
            stmt.setString(1, announcement.getTitle());
            stmt.setString(2, announcement.getText());
            stmt.setDate(3, sqlDate(announcement.getDate()));
            stmt.setBoolean(4, announcement.isHidden());
            stmt.setInt(5, announcement.getId());
            stmt.executeUpdate();
        }
    }

    public void hide(Announcement announcement) throws SQLException
    {
        try(PreparedStatement stmt = learnweb.getConnection().prepareStatement("UPDATE lw_news SET hidden = ?  WHERE news_id = ?"))
        {
            stmt.setBoolean(1, announcement.isHidden());
            stmt.setInt(2, announcement.getId());
            stmt.executeUpdate();
            log.debug(stmt.toString());
        }
    }

    private static java.sql.Date sqlDate(java.util.Date calendarDate)
    {
        return new java.sql.Date(calendarDate.getTime());
    }

    public List<Announcement> getAnnouncementsAll()
    {
        List<Announcement> newList = new ArrayList<Announcement>();
        try(ResultSet resultSet = learnweb.getConnection().createStatement().executeQuery("SELECT * FROM lw_news ORDER BY created_at DESC"))
        {
            while(resultSet.next())
            {
                Announcement announcement = createAnnouncement(resultSet);
                newList.add(announcement);
            }
        }
        catch(Exception e)
        {
            log.error(e);
        }
        return newList;
    }

    public Announcement getAnnouncementById(int newsId) throws SQLException
    {
        PreparedStatement stmt = learnweb.getConnection().prepareStatement("SELECT * FROM lw_news WHERE news_id = ?");
        stmt.setInt(1, newsId);
        try(ResultSet resultSet = stmt.executeQuery())
        {
            resultSet.next();
            return createAnnouncement(resultSet);
        }
    }

    /**
     *
     * @param maxAnnouncements
     * @return The x newest announcements that are not hidden
     */
    public List<Announcement> getTopAnnouncements(int maxAnnouncements) throws SQLException
    {
        resetCache(maxAnnouncements);
        return new ArrayList<Announcement>(cache.values());
    }

}
