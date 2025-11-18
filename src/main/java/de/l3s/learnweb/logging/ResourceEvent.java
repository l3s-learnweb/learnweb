package de.l3s.learnweb.logging;

import de.l3s.learnweb.resource.AbstractResource;

/**
 * An event about a resource or a folder.
 */
public class ResourceEvent extends ActivityEvent {
    public ResourceEvent(Action action, AbstractResource resource) {
        super(action, resource.getGroupId(), resource.getId());
    }
}
