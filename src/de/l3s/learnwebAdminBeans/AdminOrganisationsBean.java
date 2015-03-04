package de.l3s.learnwebAdminBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;

import javax.faces.application.FacesMessage;

import de.l3s.learnweb.Organisation;
import de.l3s.learnwebBeans.ApplicationBean;

//----------------------
// NOT USED AT THE MOMEMNT

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
		try {
			getLearnweb().getOrganisationManager().save(selectedOrganisation);
			
			organisations = getLearnweb().getOrganisationManager().getOrganisationsAll(); // reload
			
			addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
		}
		catch (SQLException e) {
			e.printStackTrace();
			
			addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
		}
	}

	public Organisation getSelectedOrganisation() {
		return selectedOrganisation;
	}

	public void setSelectedOrganisation(Organisation selectedOrganisation) {
		this.selectedOrganisation = selectedOrganisation;
	}

	public Collection<Organisation> getOrganisations() {
		return organisations;
	}
	
	
}
