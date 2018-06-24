package de.l3s.learnweb.resource.glossary;

public class GlossaryHelper
{
    GlossaryEntry_NEW entry;
    GlossaryTerm term;

    public String getTopicOne()
    {
        return entry.getTopicOne();
    }

    public String getTopicTwo()
    {
        return entry.getTopicTwo();
    }

    public String getTopicThree()
    {
        return entry.getTopicThree();
    }

    public String getDescription()
    {
        return entry.getDescription();
    }

    public String getTerm()
    {
        return term.getTerm();
    }

    public String getLanguage()
    {
        return term.getLanguage().getDisplayLanguage();
    }

    public String getUses()
    {
        return String.join(", ", term.getUses());
    }

    public String getPronounciation()
    {
        return term.getPronounciation();
    }

    public String getAcronym()
    {
        return term.getAcronym();
    }

    public String getSource()
    {
        return term.getSource();
    }

    public String getPhraseology()
    {
        return term.getPhraseology();
    }
}
