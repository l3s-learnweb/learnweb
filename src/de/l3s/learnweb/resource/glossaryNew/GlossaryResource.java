package de.l3s.learnweb.resource.glossaryNew;

import java.util.List;
import java.util.Locale;

public class GlossaryResource
{
    private List<Locale> allowedLanguages;

    private List<GlossaryEntry> entries;

    /*
     * I don't undestand this method. When will it be called and why?
     *
    public void getGlossaryEntries(int id)
    {
        //TODO:: get glossary entries
    }
    */
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
