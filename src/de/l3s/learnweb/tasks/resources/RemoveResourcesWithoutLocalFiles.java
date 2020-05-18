package de.l3s.learnweb.tasks.resources;

import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.FileManager;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;

/**
 * Read all files uploaded by a user, remove if there is no local copy of a file.
 *
 * @author Astappiev
 */
public class RemoveResourcesWithoutLocalFiles {
    private static final Logger log = LogManager.getLogger(RemoveResourcesWithoutLocalFiles.class);

    public static void main(String[] args) throws Exception {
        Learnweb learnweb = Learnweb.createInstance();

        ResourceManager resourceManager = learnweb.getResourceManager();
        FileManager fileManager = learnweb.getFileManager();
        resourceManager.setReindexMode(true);

        HashSet<Resource> brokenResources = new HashSet<>();
        List<Resource> resources = resourceManager.getResourcesByUserId(9289);

        log.debug("Process resources: " + resources.size());

        long totalSize = 0;
        for (Resource resource : resources) {
            try {
                List<File> files = fileManager.getFilesByResource(resource.getId());
                for (File file : files) {
                    totalSize += file.getLength();
                }
            } catch (Exception e) {
                brokenResources.add(resource);
            }
        }

        log.debug("Total using space: " + (totalSize / 1_000_000) + " MB.");
        log.debug("Total broken: " + brokenResources.size());
        for (Resource resource : brokenResources) {
            log.debug(resource.getTitle());
            // resource.delete();
        }

        learnweb.onDestroy();
    }

}
