package de.l3s.learnweb.logging;

import java.io.Serial;

import de.l3s.learnweb.resource.glossary.GlossaryEntry;
import de.l3s.learnweb.resource.glossary.GlossaryResource;
import de.l3s.learnweb.resource.glossary.GlossaryTerm;

public class LearnwebGlossaryEvent extends LearnwebResourceEvent {
    @Serial
    private static final long serialVersionUID = -2197376594983748879L;

    private GlossaryEntry entry;
    private GlossaryTerm term;

    public LearnwebGlossaryEvent(Action action, GlossaryResource resource) {
        super(action, resource);
    }

    public GlossaryEntry getEntry() {
        return entry;
    }

    public LearnwebGlossaryEvent setEntry(final GlossaryEntry entry) {
        this.entry = entry;
        return this;
    }

    public GlossaryTerm getTerm() {
        return term;
    }

    public LearnwebGlossaryEvent setTerm(final GlossaryTerm term) {
        this.term = term;
        return this;
    }
}
