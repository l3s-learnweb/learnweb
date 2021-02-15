package de.l3s.maintenance.resources;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.maintenance.MaintenanceTask;

/**
 * @author Philipp Kemkes
 */
public class RemoveDescriptionsFromSubmittedResources extends MaintenanceTask {

    @Override
    protected void init() {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) {
        if (!dryRun) {
            User submitAdmin = getLearnweb().getDaoProvider().getUserDao().findById(11212);

            log.debug(submitAdmin);
            for (Resource resource : submitAdmin.getResources()) {
                log.debug(resource);
                resource.setMachineDescription(resource.getDescription());
                resource.setDescription("");
                resource.save();
            }
        }
    }

    public static void main(String[] args) {
        new RemoveDescriptionsFromSubmittedResources().start(args);
    }
}
