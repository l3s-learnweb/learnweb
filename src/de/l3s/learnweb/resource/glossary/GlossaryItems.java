package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

// TODO this is only a helper class to generate the table the data should be stored in classes that reflect the table structure
public class GlossaryItems implements Serializable
{
    private static final long serialVersionUID = -5692141840144862371L;

    private String topic1;
    private String topic2;
    private String topic3;
    private String description;

    private int glossId; // TODO rename to glossaryId

    private List<LanguageItem> finalItems;

    private String value; // TODO rename to term

    private String pronounciation;
    private String acronym;
    private String references; // TODO in the frontend it is called source
    private String phraseology;
    private String selectedUses; // TODO uses
    private String language;
    private int termId;
    private String fulltext; // contains all text fields from all entries of this glossary to facilitate search
    private Timestamp date; // TODO use java.util.date

    public String getTopic1()
    {
        return topic1;
    }

    public void setTopic1(String topic_1)
    {
        this.topic1 = topic_1;
    }

    public String getTopic2()
    {
        return topic2;
    }

    public void setTopic2(String topic2)
    {
        this.topic2 = topic2;
    }

    public String getTopic3()
    {
        return topic3;
    }

    public void setTopic3(String topic_3)
    {
        this.topic3 = topic_3;
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
        this.finalItems = new ArrayList<LanguageItem>(); // nonsense
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

    public void setValue(String value) // TODO rename
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

    public String getLanguageNice() // quick fix for strange language format
    {
        return language.substring(0, language.indexOf("-"));
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

    public Timestamp getDate()
    {
        return date;
    }

    public void setDate(Timestamp date)
    {
        this.date = date;
    }

    public String getFulltext()
    {
        return fulltext;
    }

    public void setFulltext(String fulltext)
    {
        this.fulltext = fulltext;
    }
}
