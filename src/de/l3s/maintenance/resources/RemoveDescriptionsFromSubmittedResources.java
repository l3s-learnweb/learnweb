package de.l3s.maintenance.resources;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.maintenance.MaintenanceTask;

/**
 * @author Philipp Kemkes
 */
public class RemoveDescriptionsFromSubmittedResources extends MaintenanceTask {

    private UserManager userManager;

    @Override
    protected void init() throws Exception {
        userManager = getLearnweb().getUserManager();
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        if (!dryRun) {
            User submitAdmin = userManager.getUser(11212);

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
