package de.l3s.learnweb.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;

public class GenerateThumbnailsForImageVideoResources
{

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     * @throws MalformedURLException
     */
    public static void main(String[] args) throws SQLException, MalformedURLException, IOException
    {

        /*
        
        ResourceManager rm = Learnweb.getInstance().getResourceManager();
        ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();
        
        String query = "SELECT *  FROM `lw_resource` WHERE `deleted` = 0 AND `max_image_url` IS NOT NULL AND `max_image_url` != -1 AND thumbnail1_file_id = 0 AND storage_type=2 and type in('video', 'image') limit 10";
        //query = "SELECT * FROM `lw_resource` WHERE `thumbnail2_file_id` != 0 and type not in( 'image','pdf')";
        
        List<Resource> resources = rm.getResources(query, null);
        log.debug("start");
        for(Resource resource : resources)
        {
        
            String url = resource.getMaxImageUrl();
        
            log.debug(url);
            url = FileInspector.checkUrl(url);
        
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
        
        }
        */
    }

}
