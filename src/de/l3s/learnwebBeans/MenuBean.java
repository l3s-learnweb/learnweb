package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
@RequestScoped
public class MenuBean extends ApplicationBean implements Serializable
{
    /**
	 * 
	 */
    private static final long serialVersionUID = -7904027575208377375L;
    //private MenuModel model;
    private Course selectCourse;

    private List<Course> courses;
    private ArrayList<Group> parents;
    private ArrayList<Group> children;
    private ArrayList<Group> addedToMenu;

    public MenuBean()
    {
	if(getUser() == null)
	    return;

	try
	{
	    courses = getUser().getCourses();
	    selectCourse = UtilBean.getUserBean().getActiveCourse();
	}
	catch(SQLException e1)
	{
	    addGrowl(FacesMessage.SEVERITY_FATAL, "Fatal error. Log out please.");
	    e1.printStackTrace();
	}
	//computeMenu();

	//        MenuItem item = new MenuItem();  
	//        item.setValue("Dynamic Menuitem 1.1");  
	//        item.setUrl("#");  
	//        submenu.getChildren().add(item);  
	//          
	//        
	//          
	//        //Second submenu  
	//        submenu = new Submenu();  
	//        submenu.setLabel("Dynamic Submenu 2");  
	//          
	//        item = new MenuItem();  
	//        item.setValue("Dynamic Menuitem 2.1");  
	//        item.setUrl("#");  
	//        submenu.getChildren().add(item);  
	//          
	//        item = new MenuItem();  
	//        item.setValue("Dynamic Menuitem 2.2");  
	//        item.setUrl("#");  
	//        submenu.getChildren().add(item);  
	//          
	//        model.addSubmenu(submenu);  
    }

    /*
    public MenuModel getModel()
    {
    computeMenu();

    return model;
    }*/

    public ArrayList<Group> getParents()
    {
	return parents;
    }

    public void setParents(ArrayList<Group> parents)
    {
	this.parents = parents;
    }

    public List<Course> getCourses()
    {
	return courses;
    }

    public void setCourses(List<Course> courses)
    {
	this.courses = courses;
    }

    public Course getSelectCourse()
    {
	return selectCourse;
    }

    public void setSelectCourse(Course selectCourse)
    {
	this.selectCourse = selectCourse;
	UtilBean.getUserBean().setActiveCourse(selectCourse);
	//computeMenu();
    }

    public ArrayList<Group> getChildren()
    {
	return children;
    }

    public void setChildren(ArrayList<Group> children)
    {
	this.children = children;
    }

    /*
        public void computeMenu()
        {
    	//TO DO need to deal with the case where user is only member of a subgroup
    	parents = new ArrayList<Group>();
    	model = new DefaultMenuModel();
    	children = new ArrayList<Group>();
    	addedToMenu = new ArrayList<Group>();

    	int courseCount = courses.size();

    	Integer groupId = getParameterInt("group_id");
    	Submenu submenu;
    	try
    	{
    	    for(Group group : getUser().getGroups())
    	    {
    		if(group.getParentGroup() == null && courseCount < 3 || group.getCourse().getId() == selectCourse.getId())
    		{
    		    addedToMenu.add(group);

    		    submenu = new Submenu();
    		    submenu.setLabel(group.getTitle());
    		    if(groupId != null && groupId.equals(group.getId()))
    			submenu.setStyle("text-decoration: underline;font-style: italic;");

    		    MenuItem item = new MenuItem();
    		    item.setValue(getLocaleMessage("overview"));
    		    item.setUrl("./group/overview.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    item = new MenuItem();
    		    item.setValue(getLocaleMessage("members"));
    		    item.setUrl("./group/members.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    item = new MenuItem();
    		    item.setValue(getLocaleMessage("presentations"));
    		    item.setUrl("./group/presentations.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    item = new MenuItem();
    		    item.setValue(getLocaleMessage("links"));
    		    item.setUrl("./group/links.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    item = new MenuItem();
    		    item.setValue(getLocaleMessage("resources"));
    		    item.setUrl("./group/resources.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    Submenu submenuSubgroups = new Submenu();
    		    submenuSubgroups.setLabel(getLocaleMessage("subgroupsLabel"));

    		    MenuItem item2 = new MenuItem();
    		    item2.setValue(getLocaleMessage("overview"));
    		    item2.setUrl("./group/subgroups.jsf?group_id=" + group.getId());
    		    submenuSubgroups.getChildren().add(item2);

    		    for(Group subgroup : group.getSubgroups())
    		    {
    			if(getUser().getGroups().contains(subgroup))
    			{
    			    addedToMenu.add(subgroup);
    			    Submenu sub = new Submenu();
    			    sub.setLabel(subgroup.getTitle());
    			    if(groupId != null && groupId.equals(subgroup.getId()))
    			    {
    				submenu.setStyle("text-decoration: underline;font-style: italic;");
    				sub.setStyle("text-decoration: underline;font-style: italic;");
    				sub.setInView(true);
    			    }

    			    MenuItem subitem = new MenuItem();
    			    subitem.setValue(getLocaleMessage("overview"));
    			    subitem.setUrl("./group/overview.jsf?group_id=" + subgroup.getId());
    			    sub.getChildren().add(subitem);
    			    subitem = new MenuItem();
    			    subitem.setValue(getLocaleMessage("members"));
    			    subitem.setUrl("./group/members.jsf?group_id=" + subgroup.getId());
    			    sub.getChildren().add(subitem);
    			    subitem = new MenuItem();
    			    subitem.setValue(getLocaleMessage("resources"));
    			    subitem.setUrl("./group/resources.jsf?group_id=" + subgroup.getId());
    			    sub.getChildren().add(subitem);
    			    subitem = new MenuItem();
    			    subitem.setValue(getLocaleMessage("links"));
    			    subitem.setUrl("./group/links.jsf?group_id=" + subgroup.getId());
    			    sub.getChildren().add(subitem);
    			    subitem = new MenuItem();
    			    subitem.setValue(getLocaleMessage("presentations"));
    			    subitem.setUrl("./group/presentations.jsf?group_id=" + subgroup.getId());
    			    sub.getChildren().add(subitem);
    			    submenuSubgroups.getChildren().add(sub);
    			}

    		    }

    		    submenu.getChildren().add(submenuSubgroups);
    		    model.addSubmenu(submenu);

    		}
    		else
    		{
    		    children.add(group);

    		}

    	    }
    	    children.removeAll(addedToMenu);
    	    for(Group group : children)
    	    {
    		if(group.getCourse().getId() == selectCourse.getId())
    		{
    		    submenu = new Submenu();
    		    submenu.setLabel(group.getTitle() + " [parent: " + group.getParentGroup().getTitle() + "]");

    		    MenuItem item = new MenuItem();
    		    item.setValue(getLocaleMessage("overview"));
    		    item.setUrl("./group/overview.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    item = new MenuItem();
    		    item.setValue(getLocaleMessage("members"));
    		    item.setUrl("./group/members.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    item = new MenuItem();
    		    item.setValue(getLocaleMessage("presentations"));
    		    item.setUrl("./group/presentations.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    item = new MenuItem();
    		    item.setValue(getLocaleMessage("resources"));
    		    item.setUrl("./group/resources.jsf?group_id=" + group.getId());
    		    submenu.getChildren().add(item);

    		    model.addSubmenu(submenu);
    		}

    	    }
    	}
    	catch(SQLException e)
    	{
    	    // TODO Auto-generated catch block
    	    e.printStackTrace();
    	}
        }
    */
    public String onCourseChange()
    {
	try
	{
	    return getTemplateDir() + "/" + getUser().getOrganisation().getWelcomePage() + "?faces-redirect=true";
	}
	catch(SQLException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return null;

    }

    public String getBannerImage() throws SQLException
    {
	String bannerImageUrl = "../resources/icon/Learnweb.png";

	if(selectCourse != null && selectCourse.getBannerImage() != null)
	    bannerImageUrl = selectCourse.getBannerImage();

	return bannerImageUrl;
    }

    public String getBannerColor()
    {
	String bannerColor = "#489a83";

	if(selectCourse != null && selectCourse.getBannerColor() != null && selectCourse.getBannerColor().length() > 3)
	    bannerColor = "#" + selectCourse.getBannerColor();

	return bannerColor;
    }

    public String getBannerLink()
    {
	try
	{
	    return getUser().getOrganisation().getWelcomePage();
	}
	catch(NullPointerException e)
	{
	    // not logged in > ignore
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	}

	return "";
    }
}
