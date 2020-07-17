package de.l3s.maintenance.resources;

import java.util.List;

import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.maintenance.MaintenanceTask;

/**
 * This task is useful when you need to remove some test resources from the database, especially broken resources.
 *
 * @author Oleh Astappiev
 */
public final class DeleteHardResources extends MaintenanceTask {
    private ResourceManager resourceManager;

    @Override
    public void init() {
        resourceManager = getLearnweb().getResourceManager();
        resourceManager.setReindexMode(true);
    }

    @Override
    public void run(boolean dryRun) throws Exception {
        List<Integer> resourceIds = List.of(229236);
        log.info("Total resources to remove {}", resourceIds.size());

        if (!dryRun) {
            for (Integer resourceId : resourceIds) {
                resourceManager.deleteResourceHard(resourceId);
                log.info("Resource {} removed!", resourceId);
            }
        }
    }

    public static void main(String[] args) {
        new DeleteHardResources().start(args);
    }
}
