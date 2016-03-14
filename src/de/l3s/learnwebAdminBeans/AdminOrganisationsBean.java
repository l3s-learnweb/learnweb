package de.l3s.learnwebAdminBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.Organisation;
import de.l3s.learnwebBeans.ApplicationBean;

//----------------------
// NOT USED AT THE MOMEMNT
@ManagedBean
@ViewScoped
public class AdminOrganisationsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4815509777068370043L;
    private Collection<Organisation> organisations;
    private Organisation selectedOrganisation;

    public AdminOrganisationsBean()
    {
	organisations = getLearnweb().getOrganisationManager().getOrganisationsAll();
    }

    public void onSave()
    {
	try
	{
	    getLearnweb().getOrganisationManager().save(selectedOrganisation);

	    organisations = getLearnweb().getOrganisationManager().getOrganisationsAll(); // reload

	    addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");

	}
	catch(SQLException e)
	{
	    e.printStackTrace();

	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public Organisation getSelectedOrganisation()
    {
	return selectedOrganisation;
    }

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

	//System.out.println(selectedId + " selectedId");
    }

    public void setSelectedOrganisationById(int id)
    {

	this.selectedOrganisation = getLearnweb().getOrganisationManager().getOrganisationById(id);
    }

    public void selectedOrganisationBySelectedId()
    {
	int id = getSelectedId();
	//System.out.println(id);
	selectedOrganisation = getLearnweb().getOrganisationManager().getOrganisationById(id);
    }

    public void setSelectedOrganisation(Organisation selectedOrganisation)
    {
	this.selectedOrganisation = selectedOrganisation;
    }

    public Collection<Organisation> getOrganisations()
    {
	return organisations;
    }

}
