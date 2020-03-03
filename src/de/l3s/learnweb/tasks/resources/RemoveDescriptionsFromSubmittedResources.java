package de.l3s.learnweb.tasks.resources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

/**
 *
 * @author Kemkes
 *
 */
public class RemoveDescriptionsFromSubmittedResources
{
    private static Logger log = Logger.getLogger(RemoveDescriptionsFromSubmittedResources.class);

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws SQLException, MalformedURLException, IOException, ClassNotFoundException
    {
        System.exit(0);

        Learnweb learnweb = Learnweb.createInstance();
        UserManager um = learnweb.getUserManager();

        User submitAdmin = um.getUser(11212);

        log.debug(submitAdmin);
        for(Resource resource : submitAdmin.getResources())
        {
            log.debug(resource);
            resource.setMachineDescription(resource.getDescription());
            resource.setDescription("");
            resource.save();
        }

        log.debug("done");

        learnweb.onDestroy();
    }

}
