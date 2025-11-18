package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ResourceEvent;

@Named
@ViewScoped
public class AddFolderBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 3716630972434428811L;

    private Folder folder;
    private Group targetGroup;
    private Folder targetFolder;

    public void create(Group targetGroup, Folder targetFolder) {
        this.folder = new Folder();
        this.targetGroup = targetGroup;
        this.targetFolder = targetFolder;
    }

    public void saveFolder() {
        if (!targetGroup.canAddResources(getUser())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "group_resources.add_not_allowed", targetGroup.getTitle());
            return;
        }

        folder.setGroupId(targetGroup.getId());
        folder.setParentFolderId(targetFolder != null ? targetFolder.getId() : 0);
        folder.setUser(getUser());
        folder.save();

        addMessage(FacesMessage.SEVERITY_INFO, "folderCreated", folder.getTitle());
        fireEvent(new ResourceEvent(Action.add_folder, folder).setParams(folder.getTitle()));
    }

    public Group getTargetGroup() {
        return targetGroup;
    }

    public Folder getTargetFolder() {
        return targetFolder;
    }

    public Folder getFolder() {
        return folder;
    }
}
