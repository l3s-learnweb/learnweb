package de.l3s.learnweb.resource.glossaryNew;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import de.l3s.learnweb.resource.Resource;

public class GlossaryResource extends Resource implements Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 8388778401614338522L;

    //constructor does nothing
    public GlossaryResource()
    {

    }

    private List<Locale> allowedLanguages;

    private List<GlossaryEntry> entries;

    public List<Locale> getAllowedLanguages()
    {
        return allowedLanguages;
    }

    public void setAllowedLanguages(List<Locale> allowedLanguages)
    {
        this.allowedLanguages = allowedLanguages;
    }

    public List<GlossaryEntry> getEntries()
    {
        return entries;
    }

}
