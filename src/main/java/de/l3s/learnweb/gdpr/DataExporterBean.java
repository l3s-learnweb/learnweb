package de.l3s.learnweb.gdpr;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.primefaces.model.StreamedContent;

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

    public StreamedContent streamUserResources() throws IOException {
        BeanAssert.hasPermission(isLoggedIn());

        return ExportManager.streamResources(getUser());
    }

    public StreamedContent streamGroupResources(final Group group) throws IOException {
        BeanAssert.hasPermission(group.canDeleteGroup(getUser()));

        return ExportManager.streamResources(group);
    }
}
