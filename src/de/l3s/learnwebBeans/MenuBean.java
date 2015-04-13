package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuModel;

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

    /*
    private ArrayList<Group> parents;
    private ArrayList<Group> children;
    private ArrayList<Group> addedToMenu;
    */

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

    private MenuModel model;

    public MenuModel getModel()
    {
	if(null == model)
	    computeMenu();

	return model;
    }

    public void computeMenu()
    {
	//TO DO need to deal with the case where user is only member of a subgroup
	//parents = new ArrayList<Group>();
	model = new DefaultMenuModel();
	//children = new ArrayList<Group>();
	//addedToMenu = new ArrayList<Group>();

	int courseCount = courses.size();

	String viewId = getFacesContext().getViewRoot().getViewId();

	Integer groupId = getParameterInt("group_id");
	DefaultSubMenu submenu;

	try
	{
	    for(Group group : getUser().getGroups())
	    {
		if(group.getParentGroup() == null && courseCount < 3 || group.getCourse().getId() == selectCourse.getId())
		{
		    //addedToMenu.add(group);
		    boolean isActiveGroup = false;

		    submenu = new DefaultSubMenu();
		    submenu.setLabel(group.getTitle());
		    submenu.setId(Integer.toString(group.getId()));

		    if(groupId != null && groupId.equals(group.getId()))
		    {
			submenu.setStyleClass("active");
			isActiveGroup = true;
		    }

		    DefaultMenuItem item = new DefaultMenuItem();
		    item.setValue(getLocaleMessage("resources"));
		    item.setUrl("./group/resources.jsf?group_id=" + group.getId());
		    if(isActiveGroup && viewId.endsWith("resources.xhtml"))
			item.setStyleClass("active");
		    submenu.addElement(item);

		    item = new DefaultMenuItem();
		    item.setValue(getLocaleMessage("overview"));
		    item.setUrl("./group/overview.jsf?group_id=" + group.getId());
		    if(isActiveGroup && viewId.endsWith("overview.xhtml"))
			item.setStyleClass("active");
		    submenu.addElement(item);

		    item = new DefaultMenuItem();
		    item.setValue(getLocaleMessage("members"));
		    item.setUrl("./group/members.jsf?group_id=" + group.getId());
		    if(isActiveGroup && viewId.endsWith("members.xhtml"))
			item.setStyleClass("active");
		    submenu.addElement(item);

		    item = new DefaultMenuItem();
		    item.setValue(getLocaleMessage("presentations"));
		    item.setUrl("./group/presentations.jsf?group_id=" + group.getId());
		    if(isActiveGroup && viewId.endsWith("presentations.xhtml"))
			item.setStyleClass("active");
		    submenu.addElement(item);

		    item = new DefaultMenuItem();
		    item.setValue(getLocaleMessage("links"));
		    item.setUrl("./group/links.jsf?group_id=" + group.getId());
		    if(isActiveGroup && viewId.endsWith("links.xhtml"))
			item.setStyleClass("active");
		    submenu.addElement(item);

		    /*
		    DefaultSubMenu submenuSubgroups = new DefaultSubMenu();
		    submenuSubgroups.setLabel(getLocaleMessage("subgroupsLabel"));

		    DefaultMenuItem item2 = new DefaultMenuItem();
		    item2.setValue(getLocaleMessage("overview"));
		    item2.setUrl("./group/subgroups.jsf?group_id=" + group.getId());
		    submenuSubgroups.addElement(item2);

		    for(Group subgroup : group.getSubgroups())
		    {
		    if(getUser().getGroups().contains(subgroup))
		    {
		        addedToMenu.add(subgroup);
		        DefaultSubMenu sub = new DefaultSubMenu(subgroup.getTitle());

		        if(groupId != null && groupId.equals(subgroup.getId()))
		        {
		    	submenu.setStyle("text-decoration: underline;font-style: italic;");
		    	
		    	//sub.setStyle("text-decoration: underline;font-style: italic;");
		    	//sub.setInView(true);
		        }

		        DefaultMenuItem subitem = new DefaultMenuItem();
		        subitem.setValue(getLocaleMessage("overview"));
		        subitem.setUrl("./group/overview.jsf?group_id=" + subgroup.getId());
		        sub.addElement(subitem);
		        subitem = new DefaultMenuItem();
		        subitem.setValue(getLocaleMessage("members"));
		        subitem.setUrl("./group/members.jsf?group_id=" + subgroup.getId());
		        sub.addElement(subitem);
		        subitem = new DefaultMenuItem();
		        subitem.setValue(getLocaleMessage("resources"));
		        subitem.setUrl("./group/resources.jsf?group_id=" + subgroup.getId());
		        sub.addElement(subitem);
		        subitem = new DefaultMenuItem();
		        subitem.setValue(getLocaleMessage("links"));
		        subitem.setUrl("./group/links.jsf?group_id=" + subgroup.getId());
		        sub.addElement(subitem);
		        subitem = new DefaultMenuItem();
		        subitem.setValue(getLocaleMessage("presentations"));
		        subitem.setUrl("./group/presentations.jsf?group_id=" + subgroup.getId());
		        sub.addElement(subitem);

		        submenuSubgroups.addElement(sub);

		    }

		    }

		    submenu.addElement(submenuSubgroups);
		    */
		    model.addElement(submenu);

		}
		/*
		else
		{
		    children.add(group);

		}*/

	    }
	    /*
	    children.removeAll(addedToMenu);
	    for(Group group : children)
	    {
	    if(group.getCourse().getId() == selectCourse.getId())
	    {
	        submenu = new DefaultSubMenu();
	        submenu.setLabel(group.getTitle() + " [parent: " + group.getParentGroup().getTitle() + "]");

	        DefaultMenuItem item = new DefaultMenuItem();
	        item.setValue(getLocaleMessage("overview"));
	        item.setUrl("./group/overview.jsf?group_id=" + group.getId());
	        submenu.addElement(item);

	        item = new DefaultMenuItem();
	        item.setValue(getLocaleMessage("members"));
	        item.setUrl("./group/members.jsf?group_id=" + group.getId());
	        submenu.addElement(item);

	        item = new DefaultMenuItem();
	        item.setValue(getLocaleMessage("presentations"));
	        item.setUrl("./group/presentations.jsf?group_id=" + group.getId());
	        submenu.addElement(item);

	        item = new DefaultMenuItem();
	        item.setValue(getLocaleMessage("resources"));
	        item.setUrl("./group/resources.jsf?group_id=" + group.getId());
	        submenu.addElement(item);

	        model.addElement(submenu);
	    }

	    }
	    */
	}
	catch(SQLException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	/*
	for(MenuElement element : model.getElements())
	{
	    DefaultSubMenu groupMenu = (DefaultSubMenu) element;

	    System.out.println("------\n" + groupMenu.getLabel());

	    for(MenuElement element2 : groupMenu.getElements())
	    {
		DefaultMenuItem link = (DefaultMenuItem) element2;

		System.out.println(link.getValue() + " - " + link.getUrl());
	    }
	}
	*/

    }

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
	String bannerImageUrl = "../resources/main-template/img/LearnwebLogo.png";

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
