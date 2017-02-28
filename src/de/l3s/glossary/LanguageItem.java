package de.l3s.glossary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LanguageItem implements Serializable
{
    private static final long serialVersionUID = 7068970099338006288L;

    private String value;
    private String pronounciation;
    private String acronym;
    private String references;
    private String phraseology;
    private List<String> selectedUses;
    private String language;
    private int termId;
    private String useLabel = "Use";

    public void updateUseLabel()
    {
        String label = "";

        List<String> useLabel = new ArrayList<String>(getSelectedUses());
        for(String u : useLabel)
        {
            label = label + u + ", ";
        }

        if(label.contains(","))
        {
            label = label.trim().substring(0, label.lastIndexOf(","));
            if(!label.trim().isEmpty())
                setUseLabel(label);

            else
            {
                setUseLabel("Use");
            }
        }

    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        if(value != null)
            this.value = value;
        else
            this.value = "";
    }

    @Override
    public String toString()
    {
        return String.format("Item[value=%s]", value);
    }

    public String getPronounciation()
    {
        return pronounciation;
    }

    public void setPronounciation(String pronounciation)
    {
        if(pronounciation != null)
            this.pronounciation = pronounciation;
        else
            this.pronounciation = "";
    }

    public String getAcronym()
    {
        return acronym;
    }

    public void setAcronym(String acronym)
    {
        if(acronym != null)
            this.acronym = acronym;
        else
            this.acronym = "";
    }

    public String getReferences()
    {
        return references;
    }

    public void setReferences(String references)
    {
        if(references != null)
            this.references = references;
        else
            this.references = "";
    }

    public String getPhraseology()
    {
        return phraseology;
    }

    public void setPhraseology(String phraseology)
    {
        if(phraseology != null)
            this.phraseology = phraseology;
        else
            this.phraseology = "";
    }

    public List<String> getSelectedUses()
    {
        return selectedUses;
    }

    public void setSelectedUses(List<String> selectedUses)
    {
        this.selectedUses = new ArrayList<String>();
        if(!selectedUses.isEmpty())
            this.selectedUses = selectedUses;

    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public int getTermId()
    {
        return termId;
    }

    public void setTermId(int termId)
    {
        this.termId = termId;
    }

    public String getUseLabel()
    {
        return useLabel;
    }

    public void setUseLabel(String useLabel)
    {
        this.useLabel = useLabel;
    }

}
