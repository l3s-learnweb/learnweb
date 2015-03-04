package de.l3s.learnwebBeans;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.util.MD5;

@ManagedBean
@RequestScoped
public class GroupFeedBean extends ApplicationBean {

	private int groupId;
	private Group group;

	private List<LogEntry> logMessages;

	private boolean loaded = false;
	
	/*
	public void preRenderView() throws SQLException, KRSMException
	{	
		load();	
		
			
	}*/
	
	
	public void load() 
	{		
		if(loaded)
			return;
		loaded = true;			
			
		int userId = -1;
		String hash = getFacesContext().getExternalContext().getRequestParameterMap().get("h");
		
		String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("group_id");
		if(temp != null && temp.length() != 0)
			groupId = Integer.parseInt(temp);
		
		temp = getFacesContext().getExternalContext().getRequestParameterMap().get("u");
		if(temp != null && temp.length() != 0)
			userId = Integer.parseInt(temp);
			
		String hashTest = MD5.hash(groupId + Learnweb.salt1 + userId + Learnweb.salt2);
		
		if(!hash.equals(hashTest))
		{
			logMessages = new ArrayList<LogEntry>(1);
			logMessages.add(new LogEntry("Invalid hash, check url"));
			return;
		}
		
		if(0 == groupId) 
		{
			logMessages = new ArrayList<LogEntry>(1);
			logMessages.add(new LogEntry("Invalid group id, check url"));
			return;
		}
		
		try {
			group = getLearnweb().getGroupManager().getGroupById(groupId);
			
			Action[] filter = new Action[]{
					Action.adding_resource,
					Action.commenting_resource,
					Action.edit_resource,
					Action.deleting_resource,
					Action.group_adding_document,
					Action.group_adding_link,
					Action.group_changing_description,
					Action.group_changing_leader,
					Action.group_changing_restriction,
					Action.group_changing_title,
					Action.group_creating,
					Action.group_deleting,
					Action.group_joining,
					Action.group_leaving,
					Action.rating_resource,
					Action.tagging_resource,
					Action.thumb_rating_resource					
			};
			
			logMessages = getLearnweb().getLogsByGroup(groupId, filter);	
	
			if(null == group) 
			{
				logMessages = new ArrayList<LogEntry>(1);
				logMessages.add(new LogEntry("Invalid group id, check url"));
				return;
			}

		}
		catch (SQLException e) {
			e.printStackTrace();
			
			logMessages = new ArrayList<LogEntry>(1);
			logMessages.add(new LogEntry(e.getMessage()));
			return;
			
			
		}

	}	

	

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	
	public Group getGroup() {
		load();
		return group;
	}

	public List<LogEntry> getLogMessages() {
		load();
		return logMessages;
	}
}
