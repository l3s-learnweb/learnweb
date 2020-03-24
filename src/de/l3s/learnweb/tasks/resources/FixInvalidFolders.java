package de.l3s.learnweb.tasks.resources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;

/**
 * Find webpage resources that have no thumbnail and create it
 *
 * @author Kemkes
 *
 */
public class FixInvalidFolders
{
    private static Logger log = Logger.getLogger(FixInvalidFolders.class);

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws SQLException, MalformedURLException, IOException, ClassNotFoundException
    {
        //System.exit(0);

        Learnweb learnweb = Learnweb.createInstance();
        ResourceManager rm = learnweb.getResourceManager();
        rm.setReindexMode(true);
        //String query = "SELECT * FROM `lw_resource` r join lw_group_folder f using(folder_id) WHERE r.`group_id` = 0 AND r.`folder_id` > 0 and f.`group_id` !=0 ";
        String query = "SELECT * FROM `lw_resource` where folder_id != 0 and folder_id not in (select folder_id from lw_group_folder)";

        List<Resource> resources = rm.getResources(query, null);
        log.debug("start");
        for(Resource resource : resources)
        {
            log.debug(resource);

            resource.setFolderId(0);

            resource.save();

        }
        log.debug("done");

        learnweb.onDestroy();
    }

}
