package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.Presentation;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.User;

@ManagedBean
@ViewScoped
public class ReEditPresentationBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 5990525657898323276L;
    private static final Logger log = Logger.getLogger(ReEditPresentationBean.class);
    private int groupId;
    private Group group;
    private List<User> members;
    private List<Resource> resources;
    private LinkedList<Resource> resourcesAll;
    private List<Resource> resourcesText = new LinkedList<Resource>();
    private List<Resource> resourcesMultimedia = new LinkedList<Resource>();
    ArrayList<Resource> titleSlide;

    private ArrayList<String> queries = new ArrayList<String>();

    private boolean loaded = false;
    private Resource selectedResource;

    private static String code;
    private String presentationName;
    private int presentationId;
    private int prevId;
    private String prevTitle;
    private int format;
    private Presentation presentation;

    public void preRenderView(ComponentSystemEvent e)
    {
	load();

	User user = getUser();
	if(null != user && null != group)
	{
	    try
	    {
		user.setActiveGroup(group);

		group.setLastVisit(user);
	    }
	    catch(Exception e1)
	    {
		e1.printStackTrace();

		addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	    }
	}
    }

    public void addPresentation()
    {
	setCode(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("code"));
	setPresentationName(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("presentationName"));
	PreparedStatement insert;
	PreparedStatement update;

	try
	{
	    if(getPresentationName().equals(prevTitle))
	    {
		update = getLearnweb().getConnection().prepareStatement("UPDATE `lw_presentation` SET `code`=? WHERE `presentation_id`=? AND `group_id`=? AND `presentation_name`=?");
		update.setString(1, getCode());
		update.setInt(2, prevId);
		update.setInt(3, getGroupId());
		update.setString(4, getPresentationName());
		update.execute();
	    }
	    else
	    {
		if(prevId > 0)
		{
		    update = getLearnweb().getConnection().prepareStatement("UPDATE `lw_presentation` SET `code`=?,`presentation_name`=? WHERE `presentation_id`=? AND `group_id`=?");
		    update.setString(1, getCode());
		    update.setString(2, getPresentationName());
		    update.setInt(3, presentationId);
		    update.setInt(4, getGroupId());
		    update.execute();
		    prevId = presentationId;
		    prevTitle = getPresentationName();
		}
		else
		{
		    insert = getLearnweb().getConnection().prepareStatement("INSERT INTO `lw_presentation`(`group_id`, `user_id`, `presentation_name`, `code`) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
		    insert.setInt(1, getGroupId());
		    insert.setInt(2, getUser().getId());
		    insert.setString(3, getPresentationName());
		    insert.setString(4, getCode());
		    insert.executeUpdate();
		    ResultSet rs1 = insert.getGeneratedKeys();
		    int newId = 0;
		    if(rs1.next())
		    {
			newId = rs1.getInt(1);

		    }
		    prevId = newId;
		    prevTitle = getPresentationName();
		}
	    }
	    addGrowl(FacesMessage.SEVERITY_INFO, "presentation_saved");
	}
	catch(SQLException e)
	{
	    log.error("Error while updating existing presentation", e);
	}

    }

    public void growl()
    {
	addGrowl(FacesMessage.SEVERITY_INFO, "presentation_saved");
    }

    public void load()
    {
	if(loaded)
	    return;
	loaded = true;

	if(0 == groupId)
	{
	    String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("group_id");
	    if(temp != null && temp.length() != 0)
		groupId = Integer.parseInt(temp);

	    if(0 == groupId)
		return;
	}

	if(0 == presentationId)
	{
	    String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("presentation_id");
	    if(temp != null && temp.length() != 0)
		presentationId = Integer.parseInt(temp);

	    if(0 == presentationId)
		return;
	}

	try
	{
	    group = getLearnweb().getGroupManager().getGroupById(groupId, false);

	    if(null == group)
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "invalid group id");
		log.debug("invalid group id");
		return;
	    }

	    members = group.getMembers();

	    presentation = retrievePresentation(presentationId, groupId);
	    prevTitle = presentation.getPresentationName();
	    prevId = presentation.getPresentationId();

	    resourcesAll = new LinkedList<Resource>();

	    for(Resource r : presentation.getResources())
	    {
		resourcesAll.add(r);
	    }

	    titleSlide = new ArrayList<Resource>();
	    Resource resource = new Resource();
	    resource.setId(1);
	    resource.setEmbeddedSize1Raw("<img src='../resources/icon/blank.png' width='1' height='50' />");
	    if(presentation.getPresentationTitle().isEmpty())
	    {
		resource.setTitle(group.getTitle());
		resourcesAll.add(resource);
	    }
	    else
	    {
		resource.setTitle(presentation.getPresentationTitle());
		resource.setSource("Previous Presentation");
		resourcesAll.addFirst(resource);
	    }

	    for(Resource r1 : group.getResources())
	    {
		Boolean present = false;
		for(Resource r2 : presentation.getResources())
		{
		    if(r1.getId() == r2.getId())
			present = true;
		}
		if(!present)
		{
		    resourcesAll.add(r1);
		}
	    }
	    //Collections.sort(resourcesAll, Resource.createTitleComparator());	
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public Presentation retrievePresentation(int pid, int groupId) throws SQLException
    {
	Presentation temp = new Presentation();
	Connection dbCon = getLearnweb().getConnection();
	PreparedStatement ps = dbCon.prepareStatement("SELECT * FROM `lw_presentation` where `presentation_id`=? AND `group_id`=?");
	ps.setInt(1, pid);
	ps.setInt(2, groupId);
	ResultSet rs = ps.executeQuery();
	if(rs.next())
	{
	    temp.setGroupId(rs.getInt(1));
	    temp.setOwnerId(rs.getInt(2));
	    temp.setPresentationId(rs.getInt(3));
	    temp.setPresentationName(rs.getString(4));
	    temp.setCode(rs.getString(5));
	    temp.setDate(rs.getDate(6));
	    temp.parseCode();
	}
	return temp;
    }

    public String present()
    {
	setCode(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("code_temp"));
	return "presentation_reedit?faces-redirect=true";
    }

    public List<User> getMembers()
    {
	return members;
    }

    public List<Resource> getResources()
    {
	return resources;
    }

    public Group getGroup()
    {
	return group;
    }

    public int getGroupId()
    {
	return groupId;
    }

    public void setGroupId(int groupId)
    {
	this.groupId = groupId;
    }

    public Resource getSelectedResource()
    {
	return selectedResource;
    }

    public void setSelectedResource(Resource selectedResource)
    {
	this.selectedResource = selectedResource;
    }

    public LinkedList<Resource> getResourcesAll()
    {
	return resourcesAll;
    }

    public List<Resource> getResourcesText()
    {
	return resourcesText;
    }

    public List<Resource> getResourcesMultimedia()
    {
	return resourcesMultimedia;
    }

    public ArrayList<String> getQueries()
    {
	return queries;
    }

    public ArrayList<Resource> getTitleSlide()
    {
	return titleSlide;
    }

    public void setTitleSlide(ArrayList<Resource> titleSlide)
    {
	this.titleSlide = titleSlide;
    }

    public ReEditPresentationBean()
    {

    }

    public String getCode()
    {
	return code;
    }

    public void setCode(String code)
    {
	if(code != null)
	    ReEditPresentationBean.code = code;
    }

    public String getPresentationName()
    {
	return presentationName;
    }

    public void setPresentationName(String presentationName)
    {
	this.presentationName = presentationName;
    }

    public int getPrevId()
    {
	return prevId;
    }

    public void setPrevId(int prevId)
    {
	this.prevId = prevId;
    }

    public String getPrevTitle()
    {
	return prevTitle;
    }

    public void setPrevTitle(String prevTitle)
    {
	this.prevTitle = prevTitle;
    }

    public int getFormat()
    {
	return format;
    }

    public void setFormat(int format)
    {
	this.format = format;
    }

    public Presentation getPresentation()
    {
	return presentation;
    }

    public void setPresentation(Presentation presentation)
    {
	this.presentation = presentation;
    }
}
