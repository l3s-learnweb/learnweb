package de.l3s.learnweb.logging;

import java.io.Serial;

import de.l3s.learnweb.group.Group;

public class LearnwebGroupEvent extends LearnwebEvent {
    @Serial
    private static final long serialVersionUID = 3514167448712540353L;

    private final Group group;

    public LearnwebGroupEvent(Action action, Group group) {
        super(action);
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }
}
