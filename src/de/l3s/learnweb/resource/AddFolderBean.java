package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.faces.view.ViewScoped;

import org.apache.commons.lang3.StringUtils;

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

    public AddFolderBean()
    {
        clearForm();
    }

    public void clearForm()
    {
        folder = new Folder();
    }

    public void addFolder() throws SQLException
    {
        if(targetGroup.canAddResources(getUser()) && StringUtils.isNotEmpty(folder.getTitle()))
        {
            folder.setGroupId(targetGroup != null ? targetGroup.getId() : 0);
            folder.setParentFolderId(targetFolder != null ? targetFolder.getId() : 0);
            folder.setUser(getUser());
            folder.save();

            log(Action.add_folder, folder.getGroupId(), folder.getId(), folder.getTitle());
            addMessage(FacesMessage.SEVERITY_INFO, "folderCreated", folder.getTitle());
        }

        clearForm();
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

    public void setFolder(Folder folder)
    {
        this.folder = folder;
    }
}
