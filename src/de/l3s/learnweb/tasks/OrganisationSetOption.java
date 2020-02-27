package de.l3s.learnweb.tasks;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.OrganisationManager;

public class OrganisationSetOption
{
    private static final Logger log = Logger.getLogger(OrganisationSetOption.class);

    private Learnweb learnweb;

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException
    {
        System.exit(-1); // comment this line if you know what you are doing

        new OrganisationSetOption();
    }

    public OrganisationSetOption()
    {
        try
        {
            learnweb = Learnweb.createInstance();
            OrganisationManager organisationManager = learnweb.getOrganisationManager();
            for(Organisation org : organisationManager.getOrganisationsAll())
            {
                org.setOption(Option.Resource_Show_Content_Annotation_Field, false);
                organisationManager.save(org);
            }
        }
        catch(Throwable e)
        {
            log.fatal(e);
        }
        finally
        {
            learnweb.onDestroy();
        }
    }
}
