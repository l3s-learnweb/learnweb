package de.l3s.maintenance.resources;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

/**
 * @author Philipp Kemkes
 */
public class RemoveDescriptionsFromSubmittedResources {
    private static final Logger log = LogManager.getLogger(RemoveDescriptionsFromSubmittedResources.class);

    /**
     *
     */
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        System.exit(0);

        Learnweb learnweb = Learnweb.createInstance();
        UserManager um = learnweb.getUserManager();

        User submitAdmin = um.getUser(11212);

        log.debug(submitAdmin);
        for (Resource resource : submitAdmin.getResources()) {
            log.debug(resource);
            resource.setMachineDescription(resource.getDescription());
            resource.setDescription("");
            resource.save();
        }

        log.debug("done");

        learnweb.onDestroy();
    }

}
