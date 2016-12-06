package de.l3s.learnwebBeans;

import java.io.Serializable;
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
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.User;

@ManagedBean
@ViewScoped
public class EditPresentationBean extends ApplicationBean implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = 5990525657898323276L;
    private static final Logger log = Logger.getLogger(EditPresentationBean.class);
    private int groupId;
    private Group group;
    private List<User> members;
    private List<Resource> resources;
    private List<Resource> resourcesAll;
    private List<Resource> resourcesText = new LinkedList<Resource>();
    // private List<Resource> resourcesMultimedia = new LinkedList<Resource>();
    ArrayList<Resource> titleSlide;

    private ArrayList<String> queries = new ArrayList<String>();

    private boolean loaded = false;
    private Resource selectedResource;

    private static String code;
    private String presentationName;
    private int prevId;
    private String prevTitle;
    private int format;

    public void preRenderView(ComponentSystemEvent event)
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
	    catch(Exception e)
	    {
		addFatalMessage(e);
	    }
	}
    }

    public void addPresentation()
    {
	setCode(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("code"));
	setPresentationName(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("presentationName"));
	PreparedStatement insert;
	PreparedStatement update;
	PreparedStatement select;
	try
	{
	    if(getPresentationName().equals(prevTitle))
	    {
		update = getLearnweb().getConnection().prepareStatement("UPDATE `lw_presentation` SET `code`=? WHERE `presentation_id`=?");
		update.setString(1, getCode());
		update.setInt(2, prevId);
		update.execute();
	    }
	    else
	    {
		int id = -1;
		select = getLearnweb().getConnection().prepareStatement("SELECT `presentation_id` FROM `lw_presentation` WHERE `user_id`=? AND `group_id`=? AND `presentation_name`=? AND `deleted`=0");
		select.setInt(1, getUser().getId());
		select.setInt(2, getGroupId());
		select.setString(3, getPresentationName());
		ResultSet rs = select.executeQuery();
		while(rs.next())
		{
		    id = rs.getInt(1);
		}
		if(id != -1)
		{
		    update = getLearnweb().getConnection().prepareStatement("UPDATE `lw_presentation` SET `code`=? WHERE `presentation_id`=?");
		    update.setString(1, getCode());
		    update.setInt(2, id);
		    update.execute();
		    prevId = id;
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
		    log.debug(newId);
		}
	    }
	    addGrowl(FacesMessage.SEVERITY_INFO, "presentation_saved");

	}
	catch(SQLException e)
	{
	    log.error("Error while updating existing presentation or creating a new one", e);
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
	    Integer temp = getParameterInt("group_id");
	    if(temp != null)
		groupId = temp.intValue();

	    if(0 == groupId)
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "Invalid group parameter");
		return;
	    }
	}
	try
	{
	    group = getLearnweb().getGroupManager().getGroupById(groupId, false); // avoid cache to get a separate instance

	    if(null == group)
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "invalid group id");
		log.debug("invalid group id");
		return;
	    }

	    members = group.getMembers();

	    //resourcesAll = group.getResources();

	    // make sure the presentations use copies of the resources
	    resourcesAll = new LinkedList<Resource>();

	    for(Resource r : group.getResources())
	    {
		Resource clone = r.clone();
		clone.setId(r.getId());
		resourcesAll.add(clone);
	    }

	    titleSlide = new ArrayList<Resource>();
	    Resource resource = new Resource();
	    resource.setTitle(group.getTitle());
	    resource.setId(1);
	    resource.setEmbeddedSize1Raw("<img src='../resources/icon/blank.png' width='1' height='50' />");
	    //titleSlide.add(resource);
	    resourcesAll.add(0, resource);

	    //Collections.sort(resourcesAll, Resource.createTitleComparator());	
	}
	catch(Exception e)
	{
	    addFatalMessage(e);
	}
    }

    public String present()
    {
	setCode(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("code_temp"));
	return "presentation_edit?faces-redirect=true";
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

    public List<Resource> getResourcesAll()
    {
	return resourcesAll;
    }

    public List<Resource> getResourcesText()
    {
	return resourcesText;
    }

    /*
        public List<Resource> getResourcesMultimedia()
        {
    	return resourcesMultimedia;
        }
    */
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

    public EditPresentationBean()
    {

    }

    public String getCode()
    {
	return code;
    }

    public void setCode(String code)
    {
	if(code != null)
	    EditPresentationBean.code = code;
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
}
