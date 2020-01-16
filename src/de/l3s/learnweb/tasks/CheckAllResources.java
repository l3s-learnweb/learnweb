package de.l3s.learnweb.tasks;

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
public class CheckAllResources
{
    private final static Logger log = Logger.getLogger(CheckAllResources.class);

    public static void main(String[] args) throws Exception
    {
        Learnweb learnweb = Learnweb.createInstance();

        final int batchSize = 5000;
        ResourceManager resourceManager = learnweb.getResourceManager();
        resourceManager.setReindexMode(true);

        for(int i = 0;; i++)
        {
            log.debug("Load page: " + i);
            List<Resource> resources = resourceManager.getResourcesAll(i, batchSize);

            if(resources.size() == 0)
            {
                log.debug("finished: last page");
                break;
            }

            log.debug("Process page: " + i);

            for(Resource resource : resources)
            {
                log.debug(resource.getLocation());
            }

        }
        learnweb.onDestroy();
    }

}
