package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.l3s.util.Sql;

public class AnnouncementsManager
{
    // private static final Logger log = LogManager.getLogger(AnnouncementsManager.class);

    private static final int MAX_TOP_ANNOUNCEMENTS = 5; // maximal number of announcements that will be shown

    private Learnweb learnweb;
    private List<Announcement> cachedTopAnnouncements;

    public AnnouncementsManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;

        resetCache();
    }

    /**
     * Reloads the top X announcements.
     *
     * The caching strategy used in this class is only acceptable for many read and very few write operations.
     * Don't copy it for other use cases!
     *
     * @throws SQLException
     */
    private void resetCache() throws SQLException
    {
        var cache = new ArrayList<Announcement>(MAX_TOP_ANNOUNCEMENTS);

        try(PreparedStatement preparedStatement = learnweb.getConnection().prepareStatement("SELECT * FROM lw_news WHERE hidden = false ORDER BY created_at DESC limit ?"))
        {
            preparedStatement.setInt(1, MAX_TOP_ANNOUNCEMENTS);
            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next())
            {
                cache.add(createAnnouncement(resultSet));
            }
        }

        cachedTopAnnouncements = Collections.unmodifiableList(cache);
    }

    /**
     *
     * @return The x newest announcements that are not hidden
     */
    public List<Announcement> getTopAnnouncements() throws SQLException
    {
        return cachedTopAnnouncements;
    }

    private static Announcement createAnnouncement(ResultSet rs) throws SQLException
    {
        var announcement = new Announcement();
        announcement.setId(rs.getInt("news_id"));
        announcement.setTitle(rs.getString("title"));
        announcement.setText(rs.getString("message"));
        announcement.setUserId(rs.getInt("user_id"));
        announcement.setDate(rs.getTimestamp("created_at"));
        announcement.setHidden(rs.getBoolean("hidden"));
        return announcement;
    }

    public void save(Announcement announcement) throws SQLException
    {
        if(announcement.getId() <= 0)
            insert(announcement);
        else
            update(announcement);

        resetCache();
    }

    public void delete(Announcement announcement) throws SQLException
    {
        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_news` WHERE news_id = ?"))
        {
            delete.setInt(1, announcement.getId());
            delete.executeUpdate();
        }
        resetCache();
    }

    private void insert(Announcement announcement) throws SQLException
    {
        try(PreparedStatement stmt = learnweb.getConnection().prepareStatement("INSERT INTO lw_news (title, message, user_id, created_at, hidden) VALUES (?, ?, ?, ?, ?)"))
        {
            stmt.setString(1, announcement.getTitle());
            stmt.setString(2, announcement.getText());
            stmt.setInt(3, announcement.getUserId());
            stmt.setTimestamp(4, Sql.convertDateTime(announcement.getDate()));
            stmt.setBoolean(5, announcement.isHidden());
            stmt.executeUpdate();

            //due to the simplified caching strategy there is no need to retrieve the generated id.
        }
    }

    private void update(Announcement announcement) throws SQLException
    {
        try(PreparedStatement stmt = learnweb.getConnection().prepareStatement("UPDATE lw_news SET title = ?, message = ?, created_at = ?, hidden = ?  WHERE news_id = ?"))
        {
            stmt.setString(1, announcement.getTitle());
            stmt.setString(2, announcement.getText());
            stmt.setTimestamp(3, Sql.convertDateTime(announcement.getDate()));
            stmt.setBoolean(4, announcement.isHidden());
            stmt.setInt(5, announcement.getId());
            stmt.executeUpdate();
        }
    }

    public List<Announcement> getAnnouncementsAll() throws SQLException
    {
        List<Announcement> newList = new ArrayList<>();
        try(ResultSet resultSet = learnweb.getConnection().createStatement().executeQuery("SELECT * FROM lw_news ORDER BY created_at DESC"))
        {
            while(resultSet.next())
            {
                Announcement announcement = createAnnouncement(resultSet);
                newList.add(announcement);
            }
        }

        return newList;
    }

    public Announcement getAnnouncementById(int newsId) throws SQLException
    {
        try(PreparedStatement stmt = learnweb.getConnection().prepareStatement("SELECT * FROM lw_news WHERE news_id = ?"))
        {
            stmt.setInt(1, newsId);
            ResultSet resultSet = stmt.executeQuery();

            if(!resultSet.next())
                return null;

            return createAnnouncement(resultSet);
        }
    }
}
