package de.l3s.glossary;

import java.util.ArrayList;
import java.util.List;

public class GlossaryItems
{
    private String topic_1;
    private String topic_2;
    private String topic_3;
    private String description;

    private int glossId;
    private List<LanguageItems> itItems;
    private List<LanguageItems> ukItems;
    private List<LanguageItems> finalItems;
    private String value;
    private String glossIdString;
    private String pronounciation;
    private String acronym;
    private String references;
    private String phraseology;
    private String selectedUses;
    private String language;
    private int termId;

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

    public List<LanguageItems> getItItems()
    {
        return itItems;
    }

    public void setItItems(List<LanguageItems> itItems)
    {
        this.itItems = itItems;
    }

    public List<LanguageItems> getUkItems()
    {
        return ukItems;
    }

    public void setUkItems(List<LanguageItems> ukItems)
    {
        this.ukItems = ukItems;
    }

    public void createFinalList()
    {
        List<LanguageItems> l = new ArrayList<LanguageItems>();
        for(LanguageItems i : ukItems)
        {
            LanguageItems temp = new LanguageItems();
            temp.setValue(i.getValue());
            temp.setAcronym(i.getAcronym());
            temp.setPhraseology(i.getPhraseology());
            temp.setPronounciation(i.getPronounciation());
            temp.setReferences(i.getReferences());
            temp.setSelectedUses(i.getSelectedUses());

            temp.setLanguage("English");

            l.add(temp);
        }
        for(LanguageItems i : itItems)
        {
            LanguageItems temp = new LanguageItems();
            temp.setValue(i.getValue());
            temp.setAcronym(i.getAcronym());
            temp.setPhraseology(i.getPhraseology());
            temp.setPronounciation(i.getPronounciation());
            temp.setReferences(i.getReferences());
            temp.setSelectedUses(i.getSelectedUses());

            temp.setLanguage("Italian");

            l.add(temp);
        }
        setFinalItems(l);

    }

    public List<LanguageItems> getFinalItems()
    {
        return finalItems;
    }

    public void setFinalItems(List<LanguageItems> finalItems)
    {
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
}
