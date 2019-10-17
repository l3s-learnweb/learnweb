package de.l3s.learnweb.component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.primefaces.model.menu.MenuElement;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.ResourceContainer;

public class ResourceContainerMenuItem extends ActiveSubMenu
{
    private static final long serialVersionUID = 348046198723315064L;
    private static final Logger log = Logger.getLogger(ResourceContainerMenuItem.class);

    private String baseUrl;
    private String foldersIcons;
    private ResourceContainer resourceContainer;
    private List<MenuElement> elements;

    public ResourceContainerMenuItem(Group group, String groupIcon, String foldersIcons, String baseUrl)
    {
        this.setValue(group.getTitle());
        this.setIcon(groupIcon);
        this.setUrl(baseUrl + "?group_id=" + group.getId());

        this.resourceContainer = group;
        this.baseUrl = baseUrl;
        this.foldersIcons = foldersIcons;
    }

    public ResourceContainerMenuItem(Folder folder, String icon, String baseUrl)
    {
        this.setValue(folder.getTitle());
        this.setIcon(icon);
        this.setUrl(baseUrl + "?group_id=" + folder.getGroupId() + "&folder_id=" + folder.getId());

        this.resourceContainer = folder;
        this.baseUrl = baseUrl;
        this.foldersIcons = icon;
    }

    @Override
    public List<MenuElement> getElements()
    {
        if(elements == null)
        {
            elements = new ArrayList<>();

            try
            {
                resourceContainer.getSubFolders().forEach(folder -> {
                    MenuElement folderMenuItem = new ResourceContainerMenuItem(folder, foldersIcons, baseUrl);
                    elements.add(folderMenuItem);
                });
            }
            catch(SQLException e)
            {
                log.error("fatal", e);
            }
        }

        return elements;
    }

    @Override
    public int getElementsCount()
    {
        if(elements == null)
        {
            getElements();
        }

        return elements.size();
    }

    @Override
    public Object getParent()
    {
        return null;
    }
}
