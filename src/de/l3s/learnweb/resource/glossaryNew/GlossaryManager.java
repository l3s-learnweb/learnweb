package de.l3s.learnweb.resource.glossaryNew;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class GlossaryManager
{
    private final static Logger log = Logger.getLogger(GlossaryManager.class);
    private final Learnweb learnweb;

    public GlossaryManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public void saveEntry(GlossaryEntry entry)
    {
        //TODO:: insert entry
    }

    public void saveTerms(List<GlossaryTerm> terms)
    {
        //TODO:: insert Terms
    }

    public void copyGlossary(int oldResourceId, int newResourceId)
    {
        //TODO:: copy glossary
    }

    public GlossaryResource getGlossaryResource(int resourceId)
    {
        return null;
        //TODO:: get glossary Resource
    }

    public List<GlossaryEntry> getGlossaryEntries(int resourceId)
    {
        return null;
        //TODO:: get entries based on resource id
    }

    public List<GlossaryTerm> getGlossaryTerms(int entryId)
    {
        return null;
        //TODO:: get glossaryTerms

    }

    public ArrayList<GlossaryTableView> convertToGlossaryTableView(GlossaryResource glossaryResource2)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
