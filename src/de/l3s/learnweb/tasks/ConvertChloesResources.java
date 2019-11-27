package de.l3s.learnweb.tasks;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;

/**
 * Reeds through all undeleted resources and performs arbitrary tests
 *
 * @author Kemkes
 *
 */
public class ConvertChloesResources
{
    private final static Logger log = Logger.getLogger(ConvertChloesResources.class);

    public static void main(String[] args) throws Exception
    {
        new ConvertChloesResources();
    }

    private Learnweb learnweb;
    private ResourceManager resourceManager;

    private ConvertChloesResources() throws Exception
    {
        learnweb = Learnweb.createInstance(null);

        resourceManager = learnweb.getResourceManager();
        resourceManager.setReindexMode(true);

        convertLanguageLevel();

        learnweb.onDestroy();
    }

    private void convertLanguageLevel() throws SQLException
    {
        log.debug("convertLanguageLevel");
        List<Resource> resources = resourceManager.getResources("SELECT distinct resource_id FROM `lw_resource_langlevel` ", null, 0);

        log.debug("process " + resources.size() + " resources");

        for(Resource resource : resources)
        {
            log.debug(resource.getLocation());
        }

    }

}
