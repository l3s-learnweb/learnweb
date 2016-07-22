package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Organisation;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@SessionScoped
public class AdminOrganisationsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4815509777068370043L;
    private static final Logger log = Logger.getLogger(AdminOrganisationsBean.class);
    private List<Organisation> organisations;
    private Organisation selectedOrganisation;

    public AdminOrganisationsBean() throws SQLException
    {
	organisations = new ArrayList<Organisation>(getLearnweb().getOrganisationManager().getOrganisationsAll());
	selectedOrganisation = getUser().getOrganisation(); // by default edit the users organization
    }

    public void onSave()
    {
	try
	{
	    getLearnweb().getOrganisationManager().save(selectedOrganisation);

	    organisations = new ArrayList<Organisation>(getLearnweb().getOrganisationManager().getOrganisationsAll()); // reload

	    addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}
    }

    public Organisation getSelectedOrganisation()
    {
	return selectedOrganisation;
    }

    /*
    public Organisation getOrganisationById(int id)
    {
    
    Organisation organisation = getLearnweb().getOrganisationManager().getOrganisationById(id);
    
    return organisation;
    }
    
    int selectedId;
    
    public int getSelectedId()
    {
    return selectedId;
    }
    
    public void setSelectedId(int selectedId)
    {
    this.selectedId = selectedId;
    }
    
    public void setSelectedOrganisationById(int id)
    {
    this.selectedOrganisation = getLearnweb().getOrganisationManager().getOrganisationById(id);
    }
    
    public void selectedOrganisationBySelectedId()
    {
    int id = getSelectedId();
    selectedOrganisation = getLearnweb().getOrganisationManager().getOrganisationById(id);
    }
    */
    public void setSelectedOrganisation(Organisation selectedOrganisation)
    {
	log.debug("select organisation: " + selectedOrganisation);
	this.selectedOrganisation = selectedOrganisation;
    }

    public List<Organisation> getOrganisations()
    {
	return organisations;
    }

}
