package de.l3s.glossary;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class GlossaryItems implements Serializable
{
    private static final long serialVersionUID = -5692141840144862371L;

    private String topic_1; // TODO remove underscores, don't forget to remove it in the corresponding xhtml file
    private String topic_2; // TODO remove underscores, don't forget to remove it in the corresponding xhtml file
    private String topic_3; // TODO remove underscores, don't forget to remove it in the corresponding xhtml file
    private String description;

    private int glossId;

    private List<LanguageItem> finalItems;
    private String primaryLanguageTerm = ""; //for ordering of terms
    private de.l3s.glossary.LanguageItem.LANGUAGE primaryLanguage;
    private de.l3s.glossary.LanguageItem.LANGUAGE secondaryLanguage;

    private String value;
    private String glossIdString;
    private String pronounciation;
    private String acronym;
    private String references;
    private String phraseology;
    private String selectedUses;
    private String language;
    private int termId;
    private Timestamp date;

    public String getTopic_1()
    {
        return topic_1;
    }

    public void setTopic_1(String topic_1)
    {
        this.topic_1 = topic_1;
    }

    public String getTopic_2()
    {
        return topic_2;
    }

    public void setTopic_2(String topic_2)
    {
        this.topic_2 = topic_2;
    }

    public String getTopic_3()
    {
        return topic_3;
    }

    public void setTopic_3(String topic_3)
    {
        this.topic_3 = topic_3;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<LanguageItem> getFinalItems()
    {
        return finalItems;
    }

    public void setFinalItems(List<LanguageItem> finalItems)
    {
        this.finalItems = new ArrayList<LanguageItem>();
        this.finalItems = finalItems;
    }

    public int getGlossId()
    {
        return glossId;
    }

    public void setGlossId(int glossId)
    {
        this.glossId = glossId;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
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

    public String getSelectedUses()
    {
        return selectedUses;
    }

    public void setSelectedUses(String selectedUses)
    {
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

    public String getGlossIdString()
    {
        return glossIdString;
    }

    public void setGlossIdString(String glossIdString)
    {
        this.glossIdString = glossIdString;
    }

    public Timestamp getDate()
    {
        return date;
    }

    public void setDate(Timestamp date)
    {
        this.date = date;
    }

    public String getPrimaryLanguageTerm()
    {
        return primaryLanguageTerm;
    }

    public void setPrimaryLanguageTerm(String englishTerm)
    {
        this.primaryLanguageTerm = englishTerm;
    }

    public de.l3s.glossary.LanguageItem.LANGUAGE getPrimaryLanguage()
    {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(de.l3s.glossary.LanguageItem.LANGUAGE primaryLanguage)
    {
        this.primaryLanguage = primaryLanguage;
    }

    public de.l3s.glossary.LanguageItem.LANGUAGE getSecondaryLanguage()
    {
        return secondaryLanguage;
    }

    public void setSecondaryLanguage(de.l3s.glossary.LanguageItem.LANGUAGE secondaryLanguage)
    {
        this.secondaryLanguage = secondaryLanguage;
    }

}
