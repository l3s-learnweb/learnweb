package de.l3s.learnweb.gdpr.beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.ExportManager;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

/**
 * Accordingly to GDPR user can be able to download all collected about him data.
 * This bean responses on request of data with
 * */
@Named
@ViewScoped
public class DataExporterBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(DataExporterBean.class);

    private static final long serialVersionUID = -505457925640299810L;
    private ExportManager exportManager;

    public DataExporterBean()
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;
        exportManager = new ExportManager(user, getLearnweb());
    }

    public void requestUserResources() throws Exception
    {
        exportManager.handleResponse( "user");
    }

    public void requestGroupResources() throws Exception
    {
        exportManager.handleResponse("group");
    }

    public int externalResourcesCount(int groupId) throws SQLException
    {
        return exportManager.getExternalResources(groupId).size();
    }

    public List<String> externalResources(int groupId) throws SQLException
    {
        return exportManager.getExternalResources(groupId);
    }
}
