package de.l3s.learnweb.beans;

import javax.faces.application.FacesMessage;

import de.l3s.learnweb.Group;
import de.l3s.learnwebBeans.ApplicationBean;

public class GroupBean extends ApplicationBean
{
	private int groupId;
	private Group group;
	
	public Group getGroup() 
	{	
		if(group != null)
			return group;
		
		if(0 == groupId) 
		{
			groupId = getParameterInt("group_id");			
		}
		try 
		{
			group = getLearnweb().getGroupManager().getGroupById(groupId);				
	
			if(null == group) 
			{
				addMessage(FacesMessage.SEVERITY_ERROR, "Invalid group id");
				return null;
			}
		}
		catch (Exception e) 
		{			
			e.printStackTrace();
			addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
		}
		return null;
	}	

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
}
