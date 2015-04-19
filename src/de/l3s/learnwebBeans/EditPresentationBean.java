package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

import com.mysql.jdbc.Statement;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.OwnerList;
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
    private int groupId;
    private Group group;
    private List<User> members;
    private List<Resource> resources;
    private OwnerList<Resource, User> resourcesAll;
    private List<Resource> resourcesText = new LinkedList<Resource>();
    private List<Resource> resourcesMultimedia = new LinkedList<Resource>();
    ArrayList<Resource> titleSlide;

    private ArrayList<String> queries = new ArrayList<String>();

    private boolean loaded = false;
    private Resource selectedResource;

    private static String code;
    private String presentationName;
    private int prevId;
    private String prevTitle;
    private int format;

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
		    System.out.println(newId);
		}
	    }
	    FacesContext context = FacesContext.getCurrentInstance();
	    context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Presentation Saved", ""));
	    System.out.println("test");
	}
	catch(SQLException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
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
	try
	{
	    group = getLearnweb().getGroupManager().getGroupById(groupId);

	    if(null == group)
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "invalid group id");
		return;
	    }

	    members = group.getMembers();

	    resourcesAll = group.getResources();

	    titleSlide = new ArrayList<Resource>();
	    Resource resource = new Resource();
	    resource.setTitle(group.getTitle());
	    resource.setId(1);
	    resource.setEmbeddedSize1Raw("<img src='../resources/icon/blank.png' width='1' height='50' />");
	    //titleSlide.add(resource);
	    resourcesAll.addFirst(resource);

	    //Collections.sort(resourcesAll, Resource.createTitleComparator());	
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public String present()
    {
	setCode(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("code_temp"));
	return "presentation?faces-redirect=true";
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

    public OwnerList<Resource, User> getResourcesAll()
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
