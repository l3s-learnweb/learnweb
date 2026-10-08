package de.l3s.learnweb.logging;

import de.l3s.learnweb.group.Group;

public class GroupEvent extends ActivityEvent {
    public GroupEvent(Action action, Group group) {
        super(action);
        setGroupId(group.getId());
    }
}
