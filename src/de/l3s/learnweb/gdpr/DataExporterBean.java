package de.l3s.learnweb.gdpr;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.ExportManager;

/**
 * Allows to download resources of a user or a group
 */
@Named
@ViewScoped
public class DataExporterBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -505457925640299810L;

    public void requestUserResources() throws IOException, SQLException
    {
        new ExportManager(getLearnweb()).handleResponse(getUser());
    }

    public void requestGroupResources(final Group group) throws IOException, SQLException
    {
        new ExportManager(getLearnweb()).handleResponse(group);
    }
}
