package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupLinksBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -3323545344023147861L;
    private static final Logger log = Logger.getLogger(GroupLinksBean.class);

    private int groupId;
    // New link form
    @NotBlank
    private String newLinkUrl;
    @NotBlank
    private String newLinkTitle;
    private String newLinkType;

    private List<Link> documentLinks;
    private List<Link> links; // the same as group.getLinks() but with a link to the forum

    private Link selectedLink;
    private Link editLink;

    private Group group;

    public void onLoad() throws SQLException
    {
        User user = getUser();
        if(null == user) // not logged in
            return;

        group = getLearnweb().getGroupManager().getGroupById(groupId);

        if(null == group)
            addInvalidParameterMessage("group_id");

        if(null != group)
        {
            user.setActiveGroup(group);
            group.setLastVisit(user);
        }
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public Group getGroup()
    {
        return group;
    }

    public boolean isMember() throws SQLException
    {
        User user = getUser();

        if(null == user)
            return false;

        if(null == group)
            return false;

        return group.isMember(user);
    }

    public void saveGmailId()
    {
        String gmailId = getParameter("gmail_id");
        try
        {
            getLearnweb().getUserManager().saveGmailId(gmailId, getUser().getId());
        }
        catch(SQLException e)
        {
            log.error("Error while inserting gmail id" + e);
        }
    }

    public List<Link> getLinks() throws SQLException
    {
        if(null == links)
            updateLinksList();

        return links;
    }

    public void setLinks(List<Link> links)
    {
        this.links = links;
    }

    private void updateLinksList() throws SQLException
    {
        documentLinks = group.getDocumentLinks();
        links = new LinkedList<>(group.getLinks());
    }

    public void onAddLink()
    {
        try
        {
            if(!group.isMember(getUser()))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "You are not a member of this group");
                return;
            }

            Link.LinkType type;

            if(!newLinkType.equals("url")) // newLinkType == google document
            {
                newLinkUrl = new GoogleDriveManager().createEmptyDocument(group.getTitle() + " - " + newLinkTitle, newLinkType).getAlternateLink();
                type = Link.LinkType.DOCUMENT;
                log(Action.group_adding_document, group.getId(), group.getId(), newLinkTitle);
            }
            else
            {
                if(newLinkUrl.startsWith("https://docs.google.com"))
                {
                    type = Link.LinkType.DOCUMENT;
                    log(Action.group_adding_document, group.getId(), group.getId(), newLinkTitle);
                }
                else
                {
                    type = Link.LinkType.LINK;
                    log(Action.group_adding_link, group.getId(), group.getId(), newLinkTitle);
                }
            }

            group.addLink(newLinkTitle, newLinkUrl, type);

            addMessage(FacesMessage.SEVERITY_INFO, "link_added");

            newLinkUrl = null;
            newLinkTitle = null;
            updateLinksList();
        }
        catch(Throwable t)
        {
            addErrorMessage(t);
        }
    }

    public String onEditLink()
    {
        try
        {
            if(!group.isMember(getUser()))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "You are not a member of this group");
            }
            else
            {
                getLearnweb().getLinkManager().save(selectedLink);
                updateLinksList();
                addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
            }
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
        return getTemplateDir() + "/group/overview.xhtml?faces-redirect=true&includeViewParams=true";
    }

    public String getNewLinkUrl()
    {
        return newLinkUrl;
    }

    public void setNewLinkUrl(String newLinkUrl)
    {
        this.newLinkUrl = newLinkUrl;
    }

    public String getNewLinkTitle()
    {
        return newLinkTitle;
    }

    public void setNewLinkTitle(String newLinkTitle)
    {
        this.newLinkTitle = newLinkTitle;
    }

    public String getNewLinkType()
    {
        return newLinkType;
    }

    public void setNewLinkType(String newLinkType)
    {
        this.newLinkType = newLinkType;
    }

    public List<Link> getDocumentLinks() throws SQLException
    {
        if(null == documentLinks)
            updateLinksList();

        return documentLinks;
    }

    public Link getSelectedLink()
    {
        return selectedLink;
    }

    public void setSelectedLink(Link selectedLink)
    {
        this.selectedLink = selectedLink;
    }

    public Link getEditLink()
    {
        return editLink;
    }

    public void setEditLink(Link editLink)
    {
        this.editLink = editLink;
    }

    public void onDeleteLinkFromGroup(int linkId)
    {
        try
        {
            group.deleteLink(linkId);

            log(Action.group_deleting_link, group.getId(), group.getId(), newLinkTitle);
            addMessage(FacesMessage.SEVERITY_INFO, "link_deleted");
            updateLinksList();
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
            addMessage(FacesMessage.SEVERITY_INFO, "sorry an error occurred");
        }
    }
}
