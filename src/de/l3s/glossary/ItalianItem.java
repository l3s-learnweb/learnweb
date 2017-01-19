package de.l3s.glossary;

import java.util.List;

public class ItalianItem
{

    private String value;
    private String use;
    private String pronounciation;
    private String acronym;
    private String references;
    private String phraseology;
    private List<String> selectedUses;

    public String getValue()
    {
	return value;
    }

    public void setValue(String value)
    {
	this.value = value;
    }

    @Override
    public String toString()
    {
	return String.format("Item[value=%s]", value);
    }

    public String getUse()
    {
	return use;
    }

    public void setUse(String use)
    {
	this.use = use;
    }

    public String getPronounciation()
    {
	return pronounciation;
    }

    public void setPronounciation(String pronounciation)
    {
	this.pronounciation = pronounciation;
    }

    public String getAcronym()
    {
	return acronym;
    }

    public void setAcronym(String acronym)
    {
	this.acronym = acronym;
    }

    public String getReferences()
    {
	return references;
    }

    public void setReferences(String references)
    {
	this.references = references;
    }

    public String getPhraseology()
    {
	return phraseology;
    }

    public void setPhraseology(String phraseology)
    {
	this.phraseology = phraseology;
    }

    public List<String> getSelectedUses()
    {
	return selectedUses;
    }

    public void setSelectedUses(List<String> selectedUses)
    {
	this.selectedUses = selectedUses;
    }

}
