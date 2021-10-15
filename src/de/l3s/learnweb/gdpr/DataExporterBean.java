package de.l3s.learnweb.gdpr;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.ExportManager;

/**
 * Allows downloading resources of a user or a group.
 */
@Named
@ViewScoped
public class DataExporterBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -505457925640299810L;

    public void requestUserResources() throws IOException {
        BeanAssert.hasPermission(isLoggedIn());

        new ExportManager(getLearnweb()).handleResponse(getUser());
    }

    public void requestGroupResources(final Group group) throws IOException {
        BeanAssert.hasPermission(group.canDeleteGroup(getUser()));

        new ExportManager(getLearnweb()).handleResponse(group);
    }
}
