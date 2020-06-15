package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.logging.Action;

@Named
@ViewScoped
public class EditFolderBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 3716630972434428811L;

    private Folder folder;

    public void commandEditFolder() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try {
            int itemId = Integer.parseInt(params.get("itemId"));

            Folder folder = getLearnweb().getGroupManager().getFolder(itemId);
            if (folder != null && folder.canEditResource(getUser())) {
                this.folder = folder;
            } else {
                addGrowl(FacesMessage.SEVERITY_ERROR, "Target folder doesn't exists or you don't have permission to edit it.");
            }
        } catch (IllegalArgumentException | SQLException e) {
            addErrorMessage(e);
        }
    }

    public void saveChanges() throws SQLException {
        BeanAsserts.hasPermission(folder.canEditResource(getUser()));

        try {
            folder.unlockResource(getUser());
            folder.save();

            log(Action.edit_folder, folder.getGroupId(), folder.getId(), folder.getTitle());
            addMessage(FacesMessage.SEVERITY_INFO, "folderUpdated", folder.getTitle());
        } catch (SQLException e) {
            addErrorMessage(e);
        }
    }

    public Folder getFolder() {
        return folder;
    }
}
