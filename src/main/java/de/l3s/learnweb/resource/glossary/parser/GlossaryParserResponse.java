package de.l3s.learnweb.resource.glossary.parser;

import java.util.Collections;
import java.util.List;

import de.l3s.learnweb.resource.glossary.GlossaryEntry;

public class GlossaryParserResponse {
    private final List<GlossaryEntry> entries;
    private final List<ParsingError> errors;

    public GlossaryParserResponse(List<GlossaryEntry> entries, List<ParsingError> errors) {
        this.entries = entries;
        this.errors = Collections.unmodifiableList(errors);
    }

    public GlossaryParserResponse(ParsingError errors) {
        this.entries = null;
        this.errors = Collections.singletonList(errors);
    }

    public List<GlossaryEntry> getEntries() {
        return entries;
    }

    public List<ParsingError> getErrors() {
        return errors;
    }

    public int getEntryCount() {
        return entries == null ? 0 : entries.size();
    }

    public boolean isSuccessful() {
        return errors.isEmpty();
    }
}
