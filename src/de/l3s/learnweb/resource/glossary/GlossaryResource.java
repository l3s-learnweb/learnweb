package de.l3s.learnweb.resource.glossary;

import java.util.List;
import java.util.Locale;

public class GlossaryResource
{

    private int resourceId;
    private List<Locale> allowedLanguages;

    /*private List<GlossaryEntry_NEW> entries = new ArrayList<GlossaryEntry_NEW>();

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

    /*public List<GlossaryEntry_NEW> getEntries()
    {
        return entries;
    }*/

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

}
