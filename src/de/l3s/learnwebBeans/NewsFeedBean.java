package de.l3s.learnwebBeans;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Comment;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.User;


@RequestScoped
@ManagedBean
public class NewsFeedBean extends ApplicationBean {
 
	public final static int LIST = 0;
	public final static int BIG = 2;
	public final static int SMALL = 1;
	ArrayList<NewsEntry> newslist;
	private int entityId;
	private Boolean userlog= false;
	private int type;
	
	
		
	public NewsFeedBean() {
		String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("group_id");
		
		type= BIG;
		if(temp != null && temp.length() != 0)
			entityId = Integer.parseInt(temp);
		else 
		{ User u= getUser();
			entityId= u.getId();
			userlog= true;
		}
		
			
	}
	private void convert() 
	{
		
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
				Action.thumb_rating_resource,	
				Action.group_removing_resource
		};
		List<LogEntry> feed= null;
		try {
			if(userlog)
				feed = getLearnweb().getLogsByUser(entityId, filter);
			else 
			 feed = getLearnweb().getLogsByGroup(entityId, filter);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(feed!=null)
		{
			newslist= new ArrayList<NewsEntry>();
			for(LogEntry l: feed)
			{
				User u = null;
				Resource r = null;
				
				try {
					u = getLearnweb().getUserManager().getUser(l.getUserId());
					r= getLearnweb().getResourceManager().getResource(l.getResourceId());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println(l.getAction().toString());
				
				int commentcount = 0;
				int tagcount= 0;
				String text= l.getDescription();
				if(l.getAction()== filter[3] || r==null)
				{
					newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, false,l.getDate()));
					continue;
				}
				try {
					if(r.getComments()!=null)
					commentcount+=r.getComments().size();
				}  catch (Exception e) {
					// TODO Auto-generated catch block
					
				}
				
				
				try {
					if(r.getTags()!=null)
						tagcount+=r.getTags().size();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					
				}
				
				
				
				if(l.getAction()== filter[0]) //add_resource
				{
					
					newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, true,l.getDate()));
					continue;

				}
				if(l.getAction()== filter[1] && commentcount>0)
				{
					Comment commenttobeadded= new Comment();
					commenttobeadded.setText("comment removed!");
					try {
						
						for(Comment c: getLearnweb().getResourceManager().getCommentsByResourceId(r.getId()))
						{
						     if(c.getId()==Integer.parseInt(l.getParams()))
						     {
						    	 commenttobeadded=c;
						     }
						}
			
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					text= text+ " with "+"<b>"+commenttobeadded.getText()+"</b>" ;
					newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, true,l.getDate()));
					continue;

				}
				if(l.getAction()== filter[15])
				{
					newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, true, l.getDate()));
					continue;

				}
				if(l.getAction()== filter[14])
				{
					newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, true,l.getDate()));
					continue;

				}
				
				newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, false,l.getDate()));
				
			}
			
		}
		
		
	}
	public ArrayList<NewsEntry> getNewslist() {
		if(null == newslist)
		{
			convert();
		}
		return newslist;
	}
	public void setNewslist(ArrayList<NewsEntry> newslist) {
		this.newslist = newslist;
	}
	public int getEntityId() {
		return entityId;
	}
	public void setEntityId(int groupId) {
		this.entityId = groupId;
	}
	
	public Boolean getUserlog() {
		return userlog;
	}
	public void setUserlog(Boolean userlog) {
		this.userlog = userlog;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
		convert();
	}	
}

			
