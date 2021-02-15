package de.l3s.maintenance.resources;

import java.util.List;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Find resources with not existing folder_id.
 *
 * @author Philipp Kemkes
 */
public class FixInvalidFolders extends MaintenanceTask {

    @Override
    protected void init() {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) {
        List<Resource> resources = getLearnweb().getDaoProvider().getJdbi().withHandle(handle -> handle
            .select("SELECT * FROM lw_resource where folder_id != 0 and folder_id not in (select folder_id from lw_group_folder)")
            .map(new ResourceDao.ResourceMapper()).list());

        log.info("Total affected {} resources", resources.size());

        for (Resource resource : resources) {
            log.debug(resource);

            if (!dryRun) {
                resource.setFolderId(0);
                resource.save();
            }
        }
    }

    public static void main(String[] args) {
        new FixInvalidFolders().start(args);
    }
}
