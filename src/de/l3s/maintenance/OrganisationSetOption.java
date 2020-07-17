package de.l3s.maintenance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.OrganisationManager;

public class OrganisationSetOption {
    private static final Logger log = LogManager.getLogger(OrganisationSetOption.class);

    private static Learnweb learnweb;

    public static void main(String[] args) {
        System.exit(-1); // comment this line if you know what you are doing

        try {
            learnweb = Learnweb.createInstance();
            OrganisationManager organisationManager = learnweb.getOrganisationManager();
            for (Organisation org : organisationManager.getOrganisationsAll()) {
                org.setOption(Option.Privacy_Proxy_enabled, false);

                organisationManager.save(org);
            }
        } catch (Throwable e) {
            log.fatal(e);
        } finally {
            learnweb.onDestroy();
        }
    }
}
