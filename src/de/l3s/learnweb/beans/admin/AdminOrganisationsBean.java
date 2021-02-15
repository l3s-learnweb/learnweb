package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.OrganisationDao;

@Named
@ViewScoped
public class AdminOrganisationsBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -4815509777068370043L;
    //private static final Logger log = LogManager.getLogger(AdminOrganisationsBean.class);

    private List<Organisation> organisations;

    @Inject
    private OrganisationDao organisationDao;

    @PostConstruct
    public void init() {
        BeanAssert.authorized(isLoggedIn());
        BeanAssert.hasPermission(getUser().isAdmin());

        organisations = organisationDao.findAll();
    }

    public List<Organisation> getOrganisations() {
        return organisations;
    }
}
