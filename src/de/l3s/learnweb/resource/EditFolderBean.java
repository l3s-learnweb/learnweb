package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.util.Faces;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.FolderDao;
import de.l3s.learnweb.logging.Action;

@Named
@ViewScoped
public class EditFolderBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 3716630972434428811L;

    private Folder folder;

    @Inject
    private FolderDao folderDao;

    public void commandEditFolder() {
        try {
            Map<String, String> params = Faces.getRequestParameterMap();
            int itemId = Integer.parseInt(params.get("itemId"));

            folder = folderDao.findByIdOrElseThrow(itemId);
            BeanAssert.validate(folder.canEditResource(getUser()), "You don't have permission to edit target folder.");
        } catch (IllegalArgumentException e) {
            addErrorMessage(e);
        }
    }

    public void saveChanges() {
        folder.unlockResource(getUser());
        folder.save();

        log(Action.edit_folder, folder.getGroupId(), folder.getId(), folder.getTitle());
        addMessage(FacesMessage.SEVERITY_INFO, "folderUpdated", folder.getTitle());
    }

    public Folder getFolder() {
        return folder;
    }
}
