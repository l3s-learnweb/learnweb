package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(80));
        this.resetCache();
    }

    public synchronized void resetCache() throws SQLException
    {
        cache.clear();

        try(ResultSet rs = learnweb.getConnection().createStatement().executeQuery("SELECT * FROM lw_news ORDER BY created_at DESC "))
        {
            while(rs.next())
            {
                Announcement announcement = createNews(rs);
                cache.put(announcement.getId(), announcement);
            }
        }
        catch(Exception e)
        {
            log.error(e);
        }

    }

    private Announcement createNews(ResultSet rs) throws SQLException
    {
        Announcement announcement = new Announcement();
        announcement.setId(rs.getInt("news_id"));
        announcement.setTitle(rs.getString("title"));
        announcement.setText(rs.getString("message"));
        announcement.setUserId(rs.getInt("user_id"));
        announcement.setDate(rs.getDate("created_at")); // TODO be careful with SQL getDate. It will really only return the date but not the time.

        return announcement;
    }

    public synchronized Announcement save(Announcement announcement) throws SQLException
    {

        try(PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("INSERT INTO lw_news (title, message, user_id) VALUES (?, ?, ?)"))
        {
            stmt.setString(1, announcement.getTitle());
            stmt.setString(2, announcement.getText());
            stmt.setInt(3, announcement.getUserId());
            log.debug(stmt.toString());
            stmt.executeUpdate();
            stmt.close();
        }
        catch(Exception e)
        {
            log.error(e);
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
        catch(Exception e)
        {
            log.error(e);
        }
        cache.remove(announcement.getId());
    }

    public synchronized void update(Announcement announcement) throws SQLException
    {

        try(PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE lw_news SET title = ?, message = ? WHERE news_id = ?"))
        {

            stmt.setString(1, announcement.getTitle());
            stmt.setString(2, announcement.getText());
            stmt.setInt(3, announcement.getId());
            log.debug(stmt.toString());
            stmt.executeUpdate();
            stmt.close();
        }
    }

    public Collection<Announcement> getAnnouncementsAll() throws SQLException
    {
        resetCache(); // TODO what is then the purpose of a cache if you reset it on every request
        return Collections.unmodifiableCollection(cache.values());
    }

    public Announcement getAnnouncementById(int newsId)
    {
        return cache.get(newsId);

    }

}
