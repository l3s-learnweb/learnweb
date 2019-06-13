package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Organisation;

@Named
@ViewScoped
public class AdminOrganisationsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4815509777068370043L;
    private static final Logger log = Logger.getLogger(AdminOrganisationsBean.class);
    private List<Organisation> organisations;

    public AdminOrganisationsBean() throws SQLException
    {
        if(getUser() == null)
            return;

        if(!getUser().isAdmin())
        {
            addAccessDeniedMessage();
            return;
        }

        log.debug("init AdminOrganisationsBean");

        organisations = new ArrayList<>(getLearnweb().getOrganisationManager().getOrganisationsAll());
    }

    public List<Organisation> getOrganisations()
    {
        return organisations;
    }
}
