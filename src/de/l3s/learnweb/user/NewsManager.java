package de.l3s.learnweb.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class NewsManager
{
    private final static Logger log = Logger.getLogger(NewsManager.class);

    private Learnweb learnweb;
    private Map<Integer, News> cache;

    public NewsManager(Learnweb learnweb) throws SQLException
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
                News news = createNews(rs);
                cache.put(news.getId(), news);
            }
        }
        catch(Exception e)
        {
            log.error(e);
        }

    }

    private News createNews(ResultSet rs) throws SQLException
    {
        News news = new News();
        news.setId(rs.getInt("news_id"));
        news.setTitle(rs.getString("title"));
        news.setText(rs.getString("message"));
        news.setUser_id(rs.getInt("user_id"));
        news.setDate(rs.getDate("created_at")); // TODO be careful with SQL getDate. It will really only return the date but not the time.

        return news;
    }

    public synchronized News save(News news) throws SQLException
    {

        try
        {
            PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("INSERT INTO lw_news (title, message, user_id) VALUES (?, ?, ?)");
            stmt.setString(1, news.getTitle());
            stmt.setString(2, news.getText());
            stmt.setInt(3, news.getUser_id());
            log.debug(stmt.toString());
            stmt.executeUpdate();
            stmt.close();
        }
        catch(Exception e)
        {
            log.error(e);
        }
        return news;
    }

    public void delete(News news) throws SQLException
    {
        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM `lw_news` WHERE news_id = ?"))
        {
            delete.setInt(1, news.getId());
            log.debug(delete.toString());
            delete.executeUpdate();
        }
        catch(Exception e)
        {
            log.error(e);
        }
        cache.remove(news.getId());
    }

    public synchronized void update(News news) throws SQLException
    {

        try // TODO must define stmt here. see https://git.l3s.uni-hannover.de/Learnweb/Learnweb/wikis/Java-Tips
        {
            PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE lw_news SET title = ?, message = ? WHERE news_id = ?");
            stmt.setString(1, news.getTitle());
            stmt.setString(2, news.getText());
            stmt.setInt(3, news.getId());
            log.debug(stmt.toString());
            stmt.executeUpdate();
            stmt.close();
        }
        catch(Exception e) // TODO not really necessary. The error will be logged anyway
        {
            log.error(e);
        }
    }

    public Collection<News> getNewsAll() throws SQLException
    {
        resetCache(); // TODO what is then the purpose of a cache if you reset it on every request
        return Collections.unmodifiableCollection(cache.values());
    }

    public News getNewsById(int newsId)
    {
        return cache.get(newsId);
    }

}
