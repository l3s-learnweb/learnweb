package de.l3s.maintenance.resources;

import java.util.List;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Remove duplicated crawled resources from TED group.
 *
 * @author Oleh Astappiev
 */
public final class RemoveDuplicatedTedResources extends MaintenanceTask {
    @Override
    protected void run(final boolean dryRun) {
        List<Resource> resources = getLearnweb().getDaoProvider().getJdbi().withHandle(handle -> handle
            .select("SELECT * FROM lw_resource r WHERE r.group_id = 862 AND r.id_at_service != '' AND "
                + "EXISTS (SELECT 1 FROM lw_resource r2 WHERE r2.group_id = 862 AND r.resource_id > r2.resource_id AND "
                + "r.resource_id <= (r2.resource_id + 5) AND r.id_at_service = r2.id_at_service) AND "
                + "NOT EXISTS ( SELECT 1 FROM lw_resource r3 WHERE r3.original_resource_id = r.resource_id)")
            .map(new ResourceDao.ResourceMapper()).list());

        log.info("Process resources: {}", resources.size());

        for (Resource resource : resources) {
            log.debug("Deleting resource {}", resource.getId());
            getLearnweb().getDaoProvider().getResourceDao().deleteHard(resource);
        }
    }

    public static void main(String[] args) {
        new RemoveDuplicatedTedResources().start(args);
    }
}
