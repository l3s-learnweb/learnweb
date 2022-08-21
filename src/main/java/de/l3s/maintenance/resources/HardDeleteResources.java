package de.l3s.maintenance.resources;

import java.util.List;

import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.maintenance.MaintenanceTask;

/**
 * This task is useful when you need to remove some test resources from the database, especially broken resources.
 *
 * @author Oleh Astappiev
 */
public final class HardDeleteResources extends MaintenanceTask {
    private ResourceDao resourceDao;

    @Override
    public void init() {
        resourceDao = getLearnweb().getDaoProvider().getResourceDao();
        requireConfirmation = true;
    }

    @Override
    public void run(boolean dryRun) {
        List<Integer> resourceIds = List.of(12604, 7156);
        log.info("Total resources to remove {}", resourceIds.size());

        if (!dryRun) {
            for (Integer resourceId : resourceIds) {
                resourceDao.findById(resourceId).ifPresent(resource -> resourceDao.deleteHard(resource));
                log.info("Resource {} removed!", resourceId);
            }
        }
    }

    public static void main(String[] args) {
        new HardDeleteResources().start(args);
    }
}
