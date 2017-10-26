package de.l3s.learnweb.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.ResourcePreviewMaker;

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

        ResourceManager rm = Learnweb.createInstance("").getResourceManager();
        ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();

        String query = "SELECT * FROM `lw_resource` WHERE `deleted` =0 AND `type` LIKE 'text' AND `format` LIKE 'text/html' AND thumbnail0_file_id =0 limit 10";
        //query = "SELECT * FROM `lw_resource` WHERE `thumbnail2_file_id` != 0 and type not in( 'image','pdf')";

        List<Resource> resources = rm.getResources(query, null);
        log.debug("start");
        for(Resource resource : resources)
        {

            String url = resource.getUrl();
            log.debug(resource.getId() + "\t" + url);

            /*
            
            if(null == url || url.contains("unavailable"))
            {
            	log.error("bild nicht erreichbar" + resource.getUrl());
            
            	url = FileInspector.checkUrl(resource.getUrl());
            
            	log.debug(url);
            	break;
            }
            
            rpm.processImage(resource, new URL(url).openStream());
            
            if(resource.getType().equalsIgnoreCase("image") && resource.getEmbeddedRaw() == null)
            	resource.setEmbeddedRaw(resource.getEmbeddedSize3());
            
            resource.save();
            */

        }
    }

}
