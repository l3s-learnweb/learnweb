package de.l3s.learnweb.resource;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;

@Named
@ViewScoped
public class AddFolderBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 3716630972434428811L;

    private Folder folder;
    private Group targetGroup;
    private Folder targetFolder;

    public void reset()
    {
        folder = new Folder();
    }

    public void addFolder()
    {
        try
        {
            if (!targetGroup.canAddResources(getUser()))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "group.you_cant_add_resource", targetGroup.getTitle());
                return;
            }

            folder.setGroupId(targetGroup.getId());
            folder.setParentFolderId(targetFolder != null ? targetFolder.getId() : 0);
            folder.setUser(getUser());
            folder.save();

            log(Action.add_folder, folder.getGroupId(), folder.getId(), folder.getTitle());

            addMessage(FacesMessage.SEVERITY_INFO, "folderCreated", folder.getTitle());
            reset();
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public void setTarget(Group targetGroup, Folder targetFolder)
    {
        this.targetGroup = targetGroup;
        this.targetFolder = targetFolder;
    }

    public Group getTargetGroup()
    {
        return targetGroup;
    }

    public Folder getTargetFolder()
    {
        return targetFolder;
    }

    public Folder getFolder()
    {
        return folder;
    }
}
