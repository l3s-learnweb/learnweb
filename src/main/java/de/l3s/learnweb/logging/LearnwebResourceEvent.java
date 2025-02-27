package de.l3s.learnweb.logging;

import java.io.Serial;

import de.l3s.learnweb.resource.Resource;

public class LearnwebResourceEvent extends LearnwebGroupEvent {
    @Serial
    private static final long serialVersionUID = -2197376594983748879L;

    private final Resource resource;

    public LearnwebResourceEvent(Action action, Resource resource) {
        super(action, resource.getGroup());
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }
}
