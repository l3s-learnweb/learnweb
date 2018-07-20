package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.util.Date;

import javax.inject.Named;
import javax.faces.view.ViewScoped;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class createSurveyBean extends ApplicationBean implements Serializable // TODO complete refactoring
{

    /**
     *
     */
    private static final long serialVersionUID = 5520250635113980279L;

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getOrganization_id()
    {
        return organization_id;
    }

    public void setOrganization_id(int organization_id)
    {
        this.organization_id = organization_id;
    }

    public Date getOpen()
    {
        return open;
    }

    public void setOpen(Date open)
    {
        this.open = open;
    }

    public Date getClose()
    {
        return close;
    }

    public void setClose(Date close)
    {
        this.close = close;
    }

    public void submit()
    {
        User u = getUser();
        createSurveyManager sm = new createSurveyManager(getLearnweb());
        sm.createSurveyResource(u.getId(), title, description, open, close);
    }

    private String title;
    private String description;
    private int organization_id;
    private Date open;
    private Date close;
}
