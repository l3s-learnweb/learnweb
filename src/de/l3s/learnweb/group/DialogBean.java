package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.Resource;

@Named
@ViewScoped
public class DialogBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6809367535421680962L;
    private static final Logger log = LogManager.getLogger(DialogBean.class);

    private List<String> internalExportableResources;
    private List<String> externalNonExportableResources;

    private void updateData(int groupId) throws SQLException
    {
        Group group = getLearnweb().getGroupManager().getGroupById(groupId);

        if(null == group)
        {
            log.error("invalid group id: " + groupId);
            return;
        }
        List<Resource> groupResources = group.getResources();
        internalExportableResources = new ArrayList<>();
        externalNonExportableResources = new ArrayList<>();
        for(Resource resource : groupResources)
        {
            String prettyPath = resource.getPrettyPath();
            String resourcesPath;
            if(null != prettyPath)
            {
                resourcesPath = prettyPath + " > " + resource.getTitle();
            }
            else
            {
                resourcesPath = resource.getTitle();
            }

            if(resource.getStorageType() == Resource.WEB_RESOURCE)
            {
                externalNonExportableResources.add(resourcesPath);
            }
            else if(resource.getStorageType() == Resource.LEARNWEB_RESOURCE)
            {
                internalExportableResources.add(resourcesPath);
            }
        }
    }

    public List<String> externalResourcesList(int groupId) throws SQLException
    {
        this.updateData(groupId);
        return externalNonExportableResources;
    }

    public int externalResourcesNum(int groupId) throws SQLException
    {
        this.updateData(groupId);
        return externalNonExportableResources.size();
    }

    public int totalResourcesNum(int groupId) throws SQLException
    {
        this.updateData(groupId);
        return internalExportableResources.size() + externalNonExportableResources.size();
    }
}
