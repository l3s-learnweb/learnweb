package de.l3s.maintenance.organisations;

import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.OrganisationManager;
import de.l3s.maintenance.MaintenanceTask;

public class OrganisationSetOption extends MaintenanceTask {

    @Override
    protected void init() throws Exception {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        if (!dryRun) {
            OrganisationManager organisationManager = getLearnweb().getOrganisationManager();
            for (Organisation org : organisationManager.getOrganisationsAll()) {
                org.setOption(Option.Privacy_Proxy_enabled, false);

                organisationManager.save(org);
            }
        }
    }

    public static void main(String[] args) {
        new OrganisationSetOption().start(args);
    }
}
