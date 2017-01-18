package de.l3s.learnweb;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.l3s.util.URL;

public class WaybackUrlManager
{
    private final static Logger log = Logger.getLogger(WaybackUrlManager.class);
    private static WaybackUrlManager instance;
    private final Learnweb learnweb;
    private PreparedStatement urlRecordUpdate;
    private PreparedStatement urlRecordInsert;
    private LoadingCache<URL, UrlRecord> cache;
    //private SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private PreparedStatement urlRecordSelect;

    // there should exist only one instance of this class because of the executor services
    protected static WaybackUrlManager getInstance(Learnweb learnweb)
    {
        if(instance == null)
        {
            instance = new WaybackUrlManager(learnweb);
        }
        return instance;
    }

    private WaybackUrlManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;

        cache = CacheBuilder.newBuilder().maximumSize(4000000).build(new CacheLoader<URL, UrlRecord>()
        {
            @Override
            public UrlRecord load(URL url) throws URISyntaxException, SQLException
            {
                UrlRecord record = getUrlRecord(url);

                if(null == record)
                    throw new IllegalAccessError();
                return record;
            }
        });
    }

    private Connection getConnection() throws SQLException
    {
        return learnweb.getConnection();
    }

    /**
     * 
     * @param url UTF8 or ASCII encoded URL
     * @return
     * @throws URISyntaxException
     * @throws SQLException
     * @throws ExecutionException
     */
    public UrlRecord getUrlRecord(String url) throws URISyntaxException, SQLException
    {
        URL asciiUrl = new URL(url);

        try
        {
            return cache.get(asciiUrl);
        }
        catch(Throwable e)
        {
            return null;
        }
        //return getUrlRecord(asciiUrl);
    }

    public UrlRecord getUrlRecord(URL asciiUrl) throws URISyntaxException, SQLException
    {
        UrlRecord record = null;

        if(null == urlRecordSelect)
            urlRecordSelect = getConnection().prepareStatement("SELECT `url_id`, `first_capture`, `last_capture`, `all_captures_fetched`, `crawl_time` FROM wb_url_new WHERE url = ?");
        urlRecordSelect.setString(1, asciiUrl.toString());
        ResultSet rs = urlRecordSelect.executeQuery();
        if(rs.next())
        {
            record = new UrlRecord();
            record.setId(rs.getLong(1));
            record.setUrl(asciiUrl);
            record.setFirstCapture(rs.getDate(2));
            record.setLastCapture(rs.getDate(3));
            record.setAllCapturesFetched(rs.getInt(4) == 1);
            record.setCrawlDate(rs.getDate(5));
        }
        //urlRecordSelect.close();

        return record;
    }

    public void saveUrlRecord(String url, Date firstCapture, Date lastCapture) throws SQLException, URISyntaxException
    {
        saveUrlRecord(new UrlRecord(new URL(url), firstCapture, lastCapture));
    }

    public void saveUrlRecord(UrlRecord record) throws SQLException
    {
        if(record.getId() == -1L) // record is not stored yet => insert it
        {
            if(null == urlRecordInsert)
                urlRecordInsert = getConnection().prepareStatement("INSERT INTO `wb_url_new` (`url`, `first_capture`, `last_capture`, `crawl_time`, all_captures_fetched) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            try//()//, Statement.RETURN_GENERATED_KEYS);)
            {
                urlRecordInsert.setString(1, record.getUrl().toString());
                urlRecordInsert.setTimestamp(2, !record.isArchived() ? null : new java.sql.Timestamp(record.getFirstCapture().getTime()));
                urlRecordInsert.setTimestamp(3, !record.isArchived() ? null : new java.sql.Timestamp(record.getLastCapture().getTime()));
                urlRecordInsert.setTimestamp(4, new java.sql.Timestamp(record.getCrawlDate().getTime()));
                urlRecordInsert.setInt(5, record.isAllCapturesFetched() ? 1 : 0);
                urlRecordInsert.executeUpdate();

                // get generated id
                ResultSet rs = urlRecordInsert.getGeneratedKeys();
                if(!rs.next())
                    throw new SQLException("database error: no id generated");
                record.setId(rs.getLong(1));

                cache.put(record.getUrl(), record);
                return;
            }
            catch(SQLIntegrityConstraintViolationException e)
            {
                // if we catch a duplicate URL error we will just continue and update this URL record
                if(e.getErrorCode() != 1062)
                    throw e;

                /*
                String msg = e.getErrorCode() + e.getMessage();
                if(msg.contains("Duplicate entry") && msg.contains("for key 'url'"))
                {
                    log.debug(msg);
                }
                else
                    throw e;
                    */
            }
        }

        // record is already stored => updated it
        Timestamp updateTimestamp = new java.sql.Timestamp(record.getCrawlDate().getTime());
        if(null == urlRecordUpdate)
            urlRecordUpdate = getConnection().prepareStatement("UPDATE `wb_url_new` SET `first_capture` = ?, `last_capture` = ?, `crawl_time` = ?, all_captures_fetched = ? WHERE `url` = ? AND `crawl_time` < ?");
        urlRecordUpdate.setTimestamp(1, !record.isArchived() ? null : new java.sql.Timestamp(record.getFirstCapture().getTime()));
        urlRecordUpdate.setTimestamp(2, !record.isArchived() ? null : new java.sql.Timestamp(record.getLastCapture().getTime()));
        urlRecordUpdate.setTimestamp(3, updateTimestamp);
        urlRecordUpdate.setInt(4, record.isAllCapturesFetched() ? 1 : 0);
        urlRecordUpdate.setString(5, record.getUrl().toString());
        urlRecordUpdate.setTimestamp(6, updateTimestamp);
        int updatedRows = urlRecordUpdate.executeUpdate();

        /*
        if(updatedRows > 0)
            log.debug("Updated " + record.getUrl());
            */
        //urlRecordUpdate.close();
    }

    public static void main(String[] args) throws SQLException, URISyntaxException, ExecutionException
    {
        Learnweb lw = Learnweb.getInstance();
        WaybackUrlManager urlManager = new WaybackUrlManager(lw);

        //urlManager.copyDataFromLearnweb();

        urlManager.copyDataFromArchiveSearch();

        lw.onDestroy();
    }

    private void copyDataFromArchiveSearch() throws SQLException, URISyntaxException, ExecutionException
    {// prometheus.kbs.uni-hannover.de localhost:3307
        Connection dbConnection = DriverManager.getConnection("jdbc:mysql://prometheus.kbs.uni-hannover.de/archive_bing_big?characterEncoding=utf8", "archive_bing", "6JC5X43K9VzmdHJY");
        dbConnection.setAutoCommit(false);
        Statement select = dbConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

        select.setFetchSize(Integer.MIN_VALUE);

        log.debug("Start copyDataFromArchiveSearch");
        //ResultSet rs = select.executeQuery("SELECT url, `first_timestamp`, `last_timestamp`, IF(crawl_time = 0, last_timestamp, crawl_time), query_id, rank FROM `url_captures_count_2` LEFT JOIN pw_result USING(query_id, rank)  ");
        ResultSet rs = select.executeQuery("SELECT url, `first_timestamp`, `last_timestamp`, crawl_time, id FROM `wayback` WHERE id > 22208129 order by id");

        log.debug("Fetch results");
        long start = System.currentTimeMillis();
        int counter = 0;
        while(rs.next())
        {
            try
            {
                int id = rs.getInt(5);
                if(++counter % 1000 == 0)
                {
                    log.debug(counter + " r/ms: " + (double) counter / (double) (System.currentTimeMillis() - start) + "; cache size: " + cache.size() + "; id: " + id);
                }

                String url = rs.getString(1);
                Date crawlDate = rs.getTimestamp(4);

                // check if url is already stored
                UrlRecord record = getUrlRecord(url);
                /*
                if(true)
                    continue;*/
                if(record == null)
                {
                    record = new UrlRecord();
                    record.setUrl(url);
                }
                else if(crawlDate.getTime() < record.getCrawlDate().getTime())
                {
                    continue; // current record is already up-to-date
                }

                record.setFirstCapture(rs.getTimestamp(2));
                record.setLastCapture(rs.getTimestamp(3));
                record.setCrawlDate(crawlDate);
                saveUrlRecord(record);

                /*
                UrlRecord record = new UrlRecord();
                record.setUrl(url);
                record.setFirstCapture(rs.getTimestamp(2));
                record.setLastCapture(rs.getTimestamp(3));
                record.setCrawlDate(rs.getTimestamp(4));
                */

                //log.debug(currentRecord);

                //
            }
            catch(URISyntaxException e)
            {
                log.error(e);
            }
        }
        select.close();
        dbConnection.close();
    }

    /**
     * Copies data from the old wb_url table to twb_url_new
     * 
     * @throws SQLException
     * @throws URISyntaxException
     */
    private void copyDataFromLearnweb() throws SQLException, URISyntaxException
    {

        PreparedStatement select = getConnection().prepareStatement("SELECT `url_id`, `first_capture`, `last_capture`, all_captures_fetched, `update_time`, url FROM wb_url");
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            try
            {
                UrlRecord record = new UrlRecord();
                record.setId(rs.getLong(1));
                record.setUrl(rs.getString(6));
                record.setFirstCapture(rs.getTimestamp(2));
                record.setLastCapture(rs.getTimestamp(3));
                record.setAllCapturesFetched(rs.getInt(4) == 1);
                record.setCrawlDate(rs.getTimestamp(5));

                log.debug("copy: " + record.getUrl() + record.getFirstCapture());
                saveUrlRecord(record); // needs a modified 
            }
            catch(URISyntaxException e)
            {
                log.error(e);
            }
        }
        select.close();

    }

    /**
     * @author Philipp
     *
     */
    public class UrlRecord
    {
        private long id = -1L;
        private URL url;
        private Date firstCapture;
        private Date lastCapture;
        private Date crawlDate = new Date();
        private boolean allCapturesFetched = false;

        protected UrlRecord()
        {
            super();
        }

        public UrlRecord(URL url, Date firstCapture, Date lastCapture)
        {
            super();
            this.url = url;
            this.firstCapture = firstCapture;
            this.lastCapture = lastCapture;
        }

        public boolean isArchived()
        {
            return firstCapture != null && lastCapture != null;
        }

        public long getId()
        {
            return id;
        }

        protected void setId(long id)
        {
            this.id = id;
        }

        public URL getUrl()
        {
            return url;
        }

        public void setUrl(String url) throws URISyntaxException
        {
            this.url = new URL(url);
        }

        public void setUrl(URL url)
        {
            this.url = url;
        }

        public Date getFirstCapture()
        {
            return firstCapture;
        }

        public void setFirstCapture(Date firstCapture)
        {
            this.firstCapture = firstCapture;
        }

        public Date getLastCapture()
        {
            return lastCapture;
        }

        public void setLastCapture(Date lastCapture)
        {
            this.lastCapture = lastCapture;
        }

        public Date getCrawlDate()
        {
            return crawlDate;
        }

        public void setCrawlDate(Date lastUpdate)
        {
            this.crawlDate = lastUpdate;
        }

        public boolean isAllCapturesFetched()
        {
            return allCapturesFetched;
        }

        public void setAllCapturesFetched(boolean allCapturesFetched)
        {
            this.allCapturesFetched = allCapturesFetched;
        }

        @Override
        public String toString()
        {
            return "UrlRecord [id=" + id + ", url=" + url + ", firstCapture=" + firstCapture + ", lastCapture=" + lastCapture + ", crawlDate=" + crawlDate + ", allCapturesFetched=" + allCapturesFetched + "]";
        }

    }

}
