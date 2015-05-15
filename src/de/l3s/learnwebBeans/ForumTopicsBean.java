package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Group;

@ManagedBean
@RequestScoped
public class ForumTopicsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final static Logger log = Logger.getLogger(ForumTopicsBean.class);

    private int groupId;

    private Group group;

    public ForumTopicsBean()
    {

    }

    public void preRenderView(ComponentSystemEvent e)
    {
	try
	{
	    loadGroup();
	}
	catch(SQLException e1)
	{
	    log.error("Cant load group", e1);
	}

	log.debug(group.getTitle());
    }

    private void loadGroup() throws SQLException
    {
	if(0 == groupId)
	{
	    String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("group_id");
	    if(temp != null && temp.length() != 0)
		groupId = Integer.parseInt(temp);

	    if(0 == groupId)
		return;
	}

	group = getLearnweb().getGroupManager().getGroupById(groupId);
    }
}
