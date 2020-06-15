package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.user.Organisation;

@Named
@ViewScoped
public class AdminOrganisationsBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -4815509777068370043L;
    private static final Logger log = LogManager.getLogger(AdminOrganisationsBean.class);

    private List<Organisation> organisations;

    public AdminOrganisationsBean() throws SQLException {
        BeanAsserts.authorized(isLoggedIn());
        BeanAsserts.hasPermission(getUser().isAdmin());

        organisations = new ArrayList<>(getLearnweb().getOrganisationManager().getOrganisationsAll());
    }

    public List<Organisation> getOrganisations() {
        return organisations;
    }
}
