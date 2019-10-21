package de.l3s.learnweb.resource.archive;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;

public class ArchiveUrlManager
{
    private final static Logger log = Logger.getLogger(ArchiveUrlManager.class);
    private static ArchiveUrlManager instance;
    private final Learnweb learnweb;

    private String archiveSaveURL;

    private ExecutorService executorService;
    private ExecutorService cdxExecutorService;

    // there should exist only one instance of this class because of the executor services
    public static ArchiveUrlManager getInstance(Learnweb learnweb)
    {
        if(instance == null)
        {
            instance = new ArchiveUrlManager(learnweb);
        }
        return instance;
    }

    private ArchiveUrlManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        archiveSaveURL = learnweb.getProperties().getProperty("INTERNET_ARCHIVE_SAVE_URL");
        //collectionId = Integer.parseInt(learnweb.getProperties().getProperty("COLLECTION_ID"));
        /*try
        {
            serviceUrlObj = new URL(archiveSaveURL);
        }
        catch(MalformedURLException e)
        {
            log.error("The archive today service URL is malformed:", e);
        }*/

        executorService = Executors.newCachedThreadPool();//new ThreadPoolExecutor(maxThreads, maxThreads, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(maxThreads * 1000, true), new ThreadPoolExecutor.CallerRunsPolicy());
        cdxExecutorService = Executors.newSingleThreadExecutor();//In order to sequentially poll the CDX server and not overload it
    }

    public void updateArchiveUrl(int fileId, int resourceId, String archiveUrl) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("UPDATE `lw_resource_archiveurl` SET `file_id` = ?  WHERE `resource_id`=? and `archive_url`=?");
        replace.setInt(1, fileId);
        replace.setInt(2, resourceId);
        replace.setString(3, archiveUrl);
        replace.executeUpdate();
        replace.close();

        log.info("Processed archiveUrl: " + archiveUrl);
    }

    class ArchiveNowWorker implements Callable<String>
    {
        Resource resource;

        public ArchiveNowWorker(Resource resource)
        {
            this.resource = resource;
        }

        @Override
        public String call() throws Exception
        {
            DateFormat responseDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

            if(resource == null)
                return "resource was NULL";

            //resource = learnweb.getResourceManager().getResource(resource.getId());

            if(resource.getArchiveUrls() != null)
            {
                int versions = resource.getArchiveUrls().size();
                if(versions > 0)
                {
                    long timeDifference = (new Date().getTime() - resource.getArchiveUrls().getLast().getTimestamp().getTime()) / 1000;
                    if(timeDifference < 300)
                        return "resource was last archived less than 5 minutes ago";
                }
            }

            String archiveURL = null, mementoDateString = null;
            try
            {
                Client client = Client.create();
                WebResource webResource = client.resource(archiveSaveURL + resource.getUrl());
                ClientResponse response = webResource.get(ClientResponse.class);

                if(response.getStatus() == HttpURLConnection.HTTP_OK)
                {
                    if(response.getHeaders().containsKey("Content-Location"))
                        archiveURL = "http://web.archive.org" + response.getHeaders().getFirst("Content-Location");
                    else
                        log.debug("Content Location not found");

                    if(response.getHeaders().containsKey("X-Archive-Orig-Date"))
                        mementoDateString = response.getHeaders().getFirst("X-Archive-Orig-Date");
                    else
                        log.debug("X-Archive-Orig-Date not found");

                    Date archiveUrlDate = null;
                    if(mementoDateString != null)
                        archiveUrlDate = responseDate.parse(mementoDateString);

                    log.debug("Archived URL:" + archiveURL + " Memento DateTime:" + mementoDateString);
                    PreparedStatement prepStmt = learnweb.getConnection().prepareStatement("INSERT into lw_resource_archiveurl(`resource_id`,`archive_url`,`timestamp`) VALUES (?,?,?)");
                    prepStmt.setInt(1, resource.getId());
                    prepStmt.setString(2, archiveURL);
                    prepStmt.setTimestamp(3, new java.sql.Timestamp(archiveUrlDate.getTime()));
                    prepStmt.executeUpdate();
                    prepStmt.close();

                    resource.addArchiveUrl(null); // TODO
                }
                else if(response.getStatus() == HttpURLConnection.HTTP_FORBIDDEN)
                {
                    if(response.getHeaders().containsKey("X-Archive-Wayback-Liveweb-Error"))
                    {
                        if(response.getHeaders().getFirst("X-Archive-Wayback-Liveweb-Error").equalsIgnoreCase("RobotAccessControlException: Blocked By Robots"))
                            return "ROBOTS_ERROR";
                    }

                    log.error("Cannot archive URL because of an error other than robots.txt for resource: " + resource.getId() + "; Response: " + InterWeb.responseToString(response));
                    return "GENERIC_ERROR";
                }
            }
            catch(SQLException e)
            {
                log.error("Error while trying to save the archived URL for resource: " + resource.getId(), e);
                return "SQL_SAVE_ERROR";
            }
            catch(ParseException e)
            {
                log.error("Error while trying to parse the response date from archive URL service for resource: " + resource.getId() + "; Date trying to parse: " + mementoDateString, e);
                return "PARSE_DATE_ERROR";
            }

            return "ARCHIVE_SUCCESS";
        }

    }

    private class CDXWorker implements Callable<String>
    {
        private ResourceDecorator resource;

        private CDXWorker(ResourceDecorator resource)
        {
            this.resource = resource;
        }

        @Override
        public String call() throws NumberFormatException, SQLException, ParseException, IOException
        {
            CDXClient cdxClient = new CDXClient();
            cdxClient.isArchived(resource);
            return null;
        }

    }

    public void checkWaybackCaptures(ResourceDecorator resource)
    {
        try
        {
            if(resource.getResource().getMetadataValue("first_timestamp") == null)
            {
                PreparedStatement pStmt = learnweb.getConnection().prepareStatement("SELECT first_capture, last_capture FROM wb_url WHERE url = ?");
                pStmt.setString(1, resource.getUrl());
                ResultSet rs = pStmt.executeQuery();
                if(rs.next())
                {
                    Timestamp first = rs.getTimestamp(1);
                    Timestamp last = rs.getTimestamp(2);
                    if(first != null && last != null) // url was checked and has captures
                    {
                        SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

                        resource.getResource().setMetadataValue("first_timestamp", waybackDateFormat.format(new Date(first.getTime())));
                        resource.getResource().setMetadataValue("last_timestamp", waybackDateFormat.format(new Date(last.getTime())));
                    }
                }
                else
                    cdxExecutorService.submit(new CDXWorker(resource));
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching wayback url capture info: ", e);
        }
        catch(RejectedExecutionException e)
        {
            log.error("Checking if executor was shutdown: " + cdxExecutorService.isShutdown());
            log.error("Executor exception while submitting new wayback capture request", e);
        }
    }

    public String addResourceToArchive(Resource resource)
    {
        String response = "";
        if(!(resource.getStorageType() == Resource.LEARNWEB_RESOURCE))
        {
            Future<String> executorResponse = executorService.submit(new ArchiveNowWorker(resource));

            try
            {
                response = executorResponse.get();
                //log.debug(response);
            }
            catch(InterruptedException e)
            {
                log.error("Execution of the thread was interrupted on a task for resource: " + resource.getId(), e);
            }
            catch(ExecutionException e)
            {
                log.error("Error while retrieving response from a task that was interrupted by an exception for resource: " + resource.getId(), e);
            }
        }
        return response;
    }

    public void onDestroy()
    {
        executorService.shutdown();
        cdxExecutorService.shutdown();
        try
        {
            //Wait for a while for currently executing tasks to terminate
            if(!executorService.awaitTermination(1, TimeUnit.MINUTES))
                executorService.shutdownNow(); //cancelling currently executing tasks
        }
        catch(InterruptedException e)
        {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        try
        {
            //Wait for a while for currently executing tasks to terminate
            if(!cdxExecutorService.awaitTermination(1, TimeUnit.SECONDS))
                cdxExecutorService.shutdownNow(); //cancelling currently executing tasks
        }
        catch(InterruptedException e)
        {
            // (Re-)Cancel if current thread also interrupted
            cdxExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

    }

}
