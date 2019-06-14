package de.l3s.learnweb.group;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.Resource;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class DialogBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6809367535421680962L;

    private GroupManager groupManager;

    private List<String> internalExportableResources;
    private List<String> externalNonExportableResources;

    public DialogBean() throws SQLException
    {
        groupManager = new GroupManager(getLearnweb());
    }

    private void updateData(int groupId) throws SQLException
    {
        List<Resource> groupResources = groupManager.getGroupById(groupId).getResources();
        internalExportableResources = new ArrayList<>();
        externalNonExportableResources = new ArrayList<>();
        for (Resource resource: groupResources)
        {
            String prettyPath = resource.getPrettyPath();
            String resourcesPath;
            if (null != prettyPath)
            {
                resourcesPath = prettyPath + " > " + resource.getTitle();
            } else
            {
                resourcesPath = resource.getTitle();
            }

            if (resource.getStorageType() == Resource.WEB_RESOURCE)
            {
                externalNonExportableResources.add(resourcesPath);
            } else if (resource.getStorageType() == Resource.LEARNWEB_RESOURCE)
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
