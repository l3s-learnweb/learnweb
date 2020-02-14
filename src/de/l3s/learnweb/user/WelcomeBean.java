package de.l3s.learnweb.user;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@RequestScoped
public class WelcomeBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4337683111157393180L;

    private String welcomeMessage;

    public WelcomeBean()
    {
        User user = getUser();
        if(null == user)
            return;

        welcomeMessage = user.getOrganisation().getWelcomeMessage();
        /*
         *     try
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
        		Action.thumb_rating_resource					
        };			
        
        for(Group group : user.getGroups())
        {
        	List<LogEntry> logMessages = getLearnweb().getLogsByGroup(group.getId(), filter, 5);	
        }
        }
        catch(SQLException e)
        {
        addFatalMessage(e);
        }
        */

    }

    public String getWelcomeMessage()
    {
        return welcomeMessage;
    }
}
