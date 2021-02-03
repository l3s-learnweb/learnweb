package de.l3s.maintenance.resources;

import java.util.List;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Remove duplicated crawled resources from TED group.
 *
 * @author Oleh Astappiev
 */
public final class RemoveDuplicatedTedResources extends MaintenanceTask {
    @Override
    protected void run(final boolean dryRun) throws Exception {
        ResourceManager resourceManager = getLearnweb().getResourceManager();

        // Select resources which have other resources with the same id_at_service and resource_id > && <= +5 (next 5 ids) and was not copied by other users
        List<Resource> resources = resourceManager.getResources("SELECT * FROM lw_resource r WHERE r.group_id = 862 AND r.id_at_service != '' AND "
            + "EXISTS (SELECT 1 FROM lw_resource r2 WHERE r2.group_id = 862 AND r.resource_id > r2.resource_id AND "
            + "r.resource_id <= (r2.resource_id + 5) AND r.id_at_service = r2.id_at_service LIMIT 1) AND "
            + "NOT EXISTS ( SELECT 1 FROM lw_resource r3 WHERE r3.original_resource_id = r.resource_id LIMIT 1)", null);

        log.info("Process resources: {}", resources.size());

        for (Resource resource : resources) {
            log.debug("Deleting resource {}", resource.getId());
            resource.deleteHard();
        }
    }

    public static void main(String[] args) {
        new RemoveDuplicatedTedResources().start(args);
    }
}
