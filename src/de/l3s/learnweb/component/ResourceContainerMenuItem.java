package de.l3s.learnweb.component;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.IResourceContainer;
import org.primefaces.model.menu.MenuElement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResourceContainerMenuItem extends ActiveSubMenu
{
    private static final long serialVersionUID = 348046198723315064L;

    private String foldersIcons;
    private IResourceContainer resourceContainer;
    private List<MenuElement> elements;

    public ResourceContainerMenuItem(Group group, String groupIcon, String foldersIcons) {
        this.setValue(group.getTitle());
        this.setIcon(groupIcon);
        this.setUrl("/lw/group/resources.jsf?group_id=" + group.getId());

        this.resourceContainer = group;
        this.foldersIcons = foldersIcons;
    }

    public ResourceContainerMenuItem(Folder folder, String icon) {
        this.setValue(folder.getTitle());
        this.setIcon(icon);
        this.setUrl("/lw/group/resources.jsf?group_id=" + folder.getGroupId() + "&folder_id=" + folder.getId());

        this.resourceContainer = folder;
        this.foldersIcons = icon;
    }

    @Override
    public List<MenuElement> getElements() {
        if (elements == null) {
            elements = new ArrayList<>();

            try
            {
                resourceContainer.getSubFolders().forEach(folder -> {
                    MenuElement folderMenuItem = new ResourceContainerMenuItem(folder, foldersIcons);
                    elements.add(folderMenuItem);
                });
            }
            catch(SQLException e)
            {
                e.printStackTrace();
            }
        }

        return elements;
    }

    @Override
    public int getElementsCount() {
        if (elements == null) {
            getElements();
        }

        return elements.size();
    }

    @Override
    public Object getParent() {
        return null;
    }
}
