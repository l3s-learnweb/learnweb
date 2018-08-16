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
    //private final static Logger log = Logger.getLogger(AddFolderBean.class);
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
            folder.setGroupId(getTargetGroupId());
            folder.setParentFolderId(getTargetFolderId());
            folder.setUser(getUser());
            folder.save();

            log(Action.add_folder, folder.getGroupId(), folder.getId(), folder.getTitle());

            addMessage(FacesMessage.SEVERITY_INFO, "folderCreated", folder.getTitle());
        }

        clearForm();
    }

    private int getTargetGroupId()
    {
        return targetGroup != null ? targetGroup.getId() : 0;
    }

    private int getTargetFolderId()
    {
        return targetFolder != null ? targetFolder.getId() : 0;
    }

    public Group getTargetGroup()
    {
        return targetGroup;
    }

    public void setTargetGroup(Group targetGroup)
    {
        this.targetGroup = targetGroup;
    }

    public Folder getTargetFolder()
    {
        return targetFolder;
    }

    public void setTargetFolder(Folder targetFolder)
    {
        this.targetFolder = targetFolder;
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
