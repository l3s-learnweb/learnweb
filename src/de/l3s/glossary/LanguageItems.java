package de.l3s.glossary;

public class LanguageItems
{

    private String value;

    private String pronounciation;
    private String acronym;
    private String references;
    private String phraseology;
    private String selectedUses;
    private String language;
    private int termId;

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

    public String getSelectedUses()
    {
        return selectedUses;
    }

    public void setSelectedUses(String string)
    {
        if(string != null)
            this.selectedUses = string;
        else
            this.selectedUses = "";
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

}
