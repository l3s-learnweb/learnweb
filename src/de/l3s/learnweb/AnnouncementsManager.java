package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class AnnouncementsManager
{
    private final static Logger log = Logger.getLogger(AnnouncementsManager.class);

    private Learnweb learnweb;
    private Map<Integer, Announcement> cache;

    public AnnouncementsManager(Learnweb learnweb) throws SQLException
    {
        super();
        this.learnweb = learnweb;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(80)); // TODO it is not necessary
        // all announcements will be requested only by very few users on the admin page. But the top announcements will be requested by many users on the frontpage
        // thus you have to cache only the top entries see getTopAnnouncements()
        this.resetCache();
    }

    public synchronized void resetCache() throws SQLException
    {
        cache.clear();

        try(ResultSet rs = learnweb.getConnection().createStatement().executeQuery("SELECT * FROM lw_news ORDER BY created_at DESC "))
        {
            while(rs.next())
            {
                Announcement announcement = createAnnouncement(rs);
                cache.put(announcement.getId(), announcement);
            }
        }
        catch(Exception e)
        {
            log.error(e);
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
            log.debug(stmt.toString());
            stmt.executeUpdate();
        }
    }

    private static java.sql.Date sqlDate(java.util.Date calendarDate)
    {
        return new java.sql.Date(calendarDate.getTime());
    }

    public Collection<Announcement> getAnnouncementsAll()
    {
        return Collections.unmodifiableCollection(cache.values());
    }

    public Announcement getAnnouncementById(int newsId)
    {
        return cache.get(newsId);
    }

    /**
     *
     * @param maxAnnouncements
     * @return The x newest announcements that are not hidden
     */
    public List<Announcement> getTopAnnouncements(int maxAnnouncements) throws SQLException
    {
        // TODO use cache
        return null;
    }

}
