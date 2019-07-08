package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class GlossaryParserResponse implements Serializable
{
    private static final long serialVersionUID = -7175633373702703896L;

    private final List<GlossaryEntry> entries;
    private final List<ParsingError> errors;

    public GlossaryParserResponse(List<GlossaryEntry> entries, List<ParsingError> errors)
    {
        super();
        this.entries = entries;
        this.errors = Collections.unmodifiableList(errors);
    }

    public List<GlossaryEntry> getEntries()
    {
        return entries;
    }

    public List<ParsingError> getErrors()
    {
        return errors;
    }

    public int getEntryCount()
    {
        return entries == null ? 0 : entries.size();
    }

    public boolean isSuccessful()
    {
        return errors.isEmpty();
    }
}
