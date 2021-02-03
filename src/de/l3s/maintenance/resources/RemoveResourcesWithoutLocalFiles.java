package de.l3s.maintenance.resources;

import java.util.HashSet;
import java.util.List;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.FileManager;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Read all files uploaded by a user, remove if there is no local copy of a file.
 *
 * @author Oleh Astappiev
 */
public class RemoveResourcesWithoutLocalFiles extends MaintenanceTask {

    @Override
    protected void run(final boolean dryRun) throws Exception {
        ResourceManager resourceManager = getLearnweb().getResourceManager();
        FileManager fileManager = getLearnweb().getFileManager();
        resourceManager.setReindexMode(true);

        HashSet<Resource> brokenResources = new HashSet<>();
        List<Resource> resources = resourceManager.getResourcesByUserId(9289);

        log.debug("Process resources: {}", resources.size());

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

        log.debug("Total using space: {} MB.", totalSize / 1_000_000);
        log.debug("Total broken: {}", brokenResources.size());
        for (Resource resource : brokenResources) {
            log.debug(resource.getTitle());
            // resource.delete();
        }
    }

    public static void main(String[] args) {
        new RemoveResourcesWithoutLocalFiles().start(args);
    }
}
