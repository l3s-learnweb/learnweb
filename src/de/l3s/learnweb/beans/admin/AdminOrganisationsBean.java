package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.OrganisationDao;

@Named
@ViewScoped
public class AdminOrganisationsBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -4815509777068370043L;
    //private static final Logger log = LogManager.getLogger(AdminOrganisationsBean.class);

    @NotBlank
    @Length(min = 2, max = 50)
    private String newOrganisationTitle;

    private List<Organisation> organisations;

    @Inject
    private OrganisationDao organisationDao;

    @PostConstruct
    public void init() {
        BeanAssert.authorized(isLoggedIn());
        BeanAssert.hasPermission(getUser().isAdmin());

        organisations = organisationDao.findAll();
    }

    public void onCreateOrganisation() {
        if (organisationDao.findByTitle(newOrganisationTitle).isPresent()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "The title is already already take by an other organisation.");
            return;
        }

        Organisation org = new Organisation(newOrganisationTitle);
        organisationDao.save(org);
        addMessage(FacesMessage.SEVERITY_INFO, "A new organisation has been created. Now you can assign courses to it.");
        init(); // update course list
    }

    public String getNewOrganisationTitle() {
        return newOrganisationTitle;
    }

    public void setNewOrganisationTitle(String newOrganisationTitle) {
        this.newOrganisationTitle = newOrganisationTitle;
    }

    public List<Organisation> getOrganisations() {
        return organisations;
    }
}
