package de.l3s.learnweb.logging;

import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;

/**
 * An event about a resource or a folder.
 * For a resource, the folder it is located in is logged as well.
 */
public class ResourceEvent extends ActivityEvent {
    public ResourceEvent(Action action, AbstractResource resource) {
        super(action);
        setGroupId(resource.getGroupId());

        switch (resource) {
            case Folder folder -> setFolderId(folder.getId());
            case Resource res -> setResourceId(res.getId()).setFolderId(res.getFolderId());
            default -> throw new IllegalArgumentException("Unsupported resource type: " + resource.getClass());
        }
    }
}
