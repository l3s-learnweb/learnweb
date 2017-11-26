package de.l3s.learnweb.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Resource.OnlineStatus;
import de.l3s.learnweb.Resource.ResourceType;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.learnweb.beans.AddResourceBean;

/**
 * Find webpage resources that have no thumbnail and create it
 * 
 * @author Kemkes
 *
 */
public class GenerateThumbnailsForWebpageResources
{
    private static Logger log = Logger.getLogger(GenerateThumbnailsForWebpageResources.class);

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws SQLException, MalformedURLException, IOException, ClassNotFoundException
    {
        //System.out.println(AddResourceBean.checkUrl("http://www.teachingideas.co.uk/"));
        //System.exit(0);

        Learnweb learnweb = Learnweb.createInstance(null);
        ResourceManager rm = learnweb.getResourceManager();
        ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();

        String query = "SELECT * FROM `lw_resource` WHERE `storage_type` =2 AND `type` LIKE 'website' AND deleted =0 AND url NOT LIKE '%learnweb%' AND online_status = 'UNKNOWN' and thumbnail0_file_id = 0 ORDER BY resource_id DESC";

        List<Resource> resources = rm.getResources(query, null);
        log.debug("start");
        for(Resource resource : resources)
        {
            resource.setType(ResourceType.website);

            String url = resource.getUrl();
            log.debug(resource.getId() + "\t" + url);

            url = AddResourceBean.checkUrl(url);

            if(url == null)
            {
                resource.setOnlineStatus(OnlineStatus.OFFLINE);
                resource.save();

                log.debug("offline");
                continue;
            }
            resource.setOnlineStatus(OnlineStatus.ONLINE);

            log.debug("online");

            if(resource.getThumbnail0().getFileId() == 0)
            {
                log.debug("create thumbnail");
                try
                {
                    rpm.processWebsite(resource);
                    resource.setFormat("text/html");
                }
                catch(Throwable t)
                {
                    log.warn("Can't create thumbnail for url: " + url, t);
                }
            }

            resource.save();

        }
        log.debug("done");

        learnweb.onDestroy();
    }

}
