package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringEscapeUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Course.Option;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Link;
import de.l3s.learnweb.Link.LinkType;
import de.l3s.learnweb.User;

@ManagedBean
@ViewScoped
public class TreeBean extends ApplicationBean implements Serializable {

	private static final long serialVersionUID = -7184079280977372154L;

	private TreeNode root;
	private boolean enableCourseLink = false;
	private TreeNode parent = null;
	private ArrayList<Link> forumList = new ArrayList<Link>();

	public ArrayList<Link> getForumList() {
		return forumList;
	}

	public void setForumList(ArrayList<Link> forumList) {
		this.forumList = forumList;
	}

	public TreeBean() {

		TreeNode courseNode = null;
		TreeNode group = null;
		//TreeNode subgroup= null;
		User user = getUser();

		root = new DefaultTreeNode("Root", null);
		
		if(null == user)
			return;
		
		try {
			if (user.getCourses().size() > 1)
				enableCourseLink = true;

			for (Course course : user.getCourses()) 
			{
				if (enableCourseLink) {
					courseNode = new DefaultTreeNode(course, root);
					courseNode.setExpanded(true);
				}

				String forumUrl = course.getForumUrl(user);
				
				if (course.getOption(Option.Course_Forum_enabled) && forumUrl != null) 
				{
					forumList.add(new Link(LinkType.LINK, course.getTitle() + " forum", forumUrl));
				}
				for (Group g : course.getGroupsFilteredByUser(user)) 
				{
					if (g.getParentGroupId() < 10) {
						//Group temp = g;
						if (enableCourseLink)
							parent = courseNode;
						else
							parent = root;

						group = new DefaultTreeNode(g, parent);
					}

					for (Group s : g.getSubgroups()) {
						if(!s.isMember(getUser()))
							continue;
						parent = null;
						// Group temp= s;
						//subgroup= 
						DefaultTreeNode level2 = new DefaultTreeNode(s, group);
						
						for(Group ss: s.getSubgroups())
						{
							if(!ss.isMember(getUser()))
								continue;
							parent = null;
							// Group temp= s;
							//subgroup= 
								new DefaultTreeNode(ss, level2);
						}
					}
					
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TreeNode getRoot() {
		return root;
	}

	public String escape(String str) {
		return StringEscapeUtils.escapeEcmaScript(str);
	}

	public boolean isCourse(Object obj) {
		if (obj instanceof Course)
			return true;
		return false;
	}

}