package de.l3s.learnweb.resource.archive;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Learnweb.SERVICE;
import de.l3s.learnweb.resource.Resource;

public class WaybackCapturesLogger
{
    private static final Logger log = Logger.getLogger(WaybackCapturesLogger.class);

    private final static Container LAST_ENTRY = new Container("", 0L, 0L); // this element indicates that the consumer thread should stop
    private final Learnweb learnweb;
    private final LinkedBlockingQueue<Container> queue;
    private final Thread consumerThread;
    private ExecutorService cdxExecutorService;

    public WaybackCapturesLogger(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        this.queue = new LinkedBlockingQueue<>();
        this.consumerThread = new Thread(new Consumer());
        this.consumerThread.start();
        this.cdxExecutorService = Executors.newSingleThreadExecutor();
    }

    public void logWaybackUrl(String url, long firstCapture, long lastCapture)
    {
        try
        {
            Container c = new Container(url, firstCapture, lastCapture);
            if(!queue.contains(c))
                queue.put(c);
        }
        catch(InterruptedException e)
        {
            log.fatal("Couldn't log wayback url capture", e);
        }
    }

    public void logWaybackCaptures(Resource resource)
    {
        if(learnweb.getService().equals(SERVICE.AMA))
            return;

        if(resource == null)
            throw new NullPointerException();

        cdxExecutorService.submit(new CDXWorker(resource));
    }

    public void stop()
    {
        try
        {
            queue.put(LAST_ENTRY);
            consumerThread.join();

            cdxExecutorService.shutdown();
            //Wait for a while for currently executing tasks to terminate
            if(!cdxExecutorService.awaitTermination(50, TimeUnit.SECONDS))
                cdxExecutorService.shutdownNow(); //cancelling currently executing tasks

            log.debug("Wayback captures executor service was stopped");
        }
        catch(InterruptedException e)
        {
            log.fatal("Couldn't stop wayback captures logger", e);
            // (Re-)Cancel if current thread also interrupted
            cdxExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private class Consumer implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                while(true)
                {
                    Container container = queue.take();

                    if(container == LAST_ENTRY) // stop method was called
                        break;

                    try
                    {
                        PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `wb_url` (`url`, `first_capture`, `last_capture`) VALUES (?, ?, ?)");
                        insert.setString(1, container.url);
                        insert.setTimestamp(2, new Timestamp(container.firstCapture));
                        insert.setTimestamp(3, new Timestamp(container.lastCapture));
                        insert.executeUpdate();

                        //log.debug("Logged suggestion: " + container);
                    }
                    catch(SQLException e)
                    {
                        log.fatal("Couldn't log wayback url capture: " + container, e);
                    }
                }

                log.debug("Wayback Captures logger thread was stopped");
            }
            catch(InterruptedException e)
            {
                log.fatal("Wayback Captures logger crashed", e);
            }
        }
    }

    private static class Container
    {
        String url;
        long firstCapture;
        long lastCapture;

        public Container(String url, long firstCapture, long lastCapture)
        {
            super();
            this.url = url;
            this.firstCapture = firstCapture;
            this.lastCapture = lastCapture;
        }

        @Override
        public String toString()
        {
            return "Container [url=" + url + ", firstCapture=" + firstCapture + ", lastCapture=" + lastCapture + "]";
        }

    }

    private class CDXWorker implements Callable<String>
    {
        Resource resource;

        public CDXWorker(Resource resource)
        {
            this.resource = resource;
        }

        @Override
        public String call() throws NumberFormatException, SQLException
        {
            CDXClient cdxClient = new CDXClient();
            List<Long> timestamps = cdxClient.getCaptures(resource.getUrl());
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT url_id FROM wb_url WHERE url = ?");
            pStmt.setString(1, resource.getUrl());
            ResultSet rs = pStmt.executeQuery();
            if(rs.next())
            {
                int urlId = rs.getInt(1);
                PreparedStatement pStmt3 = learnweb.getConnection().prepareStatement("UPDATE wb_url SET all_captures_fetched = 1 WHERE url_id = ?");
                PreparedStatement pStmt2 = learnweb.getConnection().prepareStatement("INSERT INTO `wb_url_capture`(`url_id`,`timestamp`) VALUES(?,?)");
                pStmt2.setInt(1, urlId);
                int batchCount = 0;
                for(long timestamp : timestamps)
                {
                    batchCount++;
                    pStmt2.setTimestamp(2, new Timestamp(timestamp));
                    pStmt2.addBatch();
                    if(batchCount % 1000 == 0 || batchCount == timestamps.size())
                        pStmt2.executeBatch();
                }
                pStmt3.setInt(1, urlId);
                pStmt3.executeUpdate();
                log.debug("Logged the wayback captures in the database for: " + resource.getUrl());
            }
            resource.addArchiveUrl(null);
            return null;
        }
    }
}
