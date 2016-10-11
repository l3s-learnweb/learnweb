package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Organisation;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class AdminOrganisationBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4337683111157393180L;
    private Organisation organisation;

    public AdminOrganisationBean()
    {
	User user = getUser();
	if(null == user)
	    return;

	try
	{
	    organisation = user.getOrganisation();
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}
    }

    public void onSave()
    {
	try
	{
	    getLearnweb().getOrganisationManager().save(organisation);
	    addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}

    }

    public Organisation getOrganisation()
    {
	return organisation;
    }
}
