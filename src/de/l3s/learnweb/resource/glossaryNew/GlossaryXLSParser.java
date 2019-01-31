package de.l3s.learnweb.resource.glossaryNew;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.primefaces.model.UploadedFile;

public class GlossaryXLSParser
{

    public GlossaryXLSParser(UploadedFile uploadedFile, HashMap<String, Locale> languageMap)
    {
        // TODO Auto-generated constructor stub
    }

    /**
     *
     * @return true if the file can be parsed without errors
     */
    public boolean isValid()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     *
     * @return the parsed glossary entries
     */
    public List<GlossaryEntry> getEntries()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getErrorMessages()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
