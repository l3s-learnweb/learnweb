package de.l3s.maintenance.organisations;

import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.maintenance.MaintenanceTask;

public class OrganisationSetOption extends MaintenanceTask {

    @Override
    protected void init() {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) {
        if (!dryRun) {
            OrganisationDao organisationDao = getLearnweb().getDaoProvider().getOrganisationDao();
            for (Organisation org : organisationDao.findAll()) {
                org.setOption(Option.Privacy_Proxy_enabled, false);

                organisationDao.save(org);
            }
        }
    }

    public static void main(String[] args) {
        new OrganisationSetOption().start(args);
    }
}
