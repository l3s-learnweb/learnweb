package de.l3s.learnweb.resource.glossary;

import java.util.List;
import java.util.Locale;

public class GlossaryTerm
{
    private int termId;
    private String term;
    private List<String> uses;
    private String pronounciation;
    private String acronym;
    private String source;
    private String phraseology;
    private Locale language;
    private boolean onPasteTerm = false;
    private boolean onPastePronounciation = false;
    private boolean onPasteAcronym = false;
    private boolean onPastePhraseology = false;

    public String getTerm()
    {
        return term;
    }

    public void setTerm(String term)
    {
        this.term = term;
    }

    public List<String> getUses()
    {
        return uses;
    }

    public void setUses(List<String> uses)
    {
        this.uses = uses;
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

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getPhraseology()
    {
        return phraseology;
    }

    public void setPhraseology(String phraseology)
    {
        this.phraseology = phraseology;
    }

    public Locale getLanguage()
    {
        return language;
    }

    public void setLanguage(Locale language)
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

    public boolean isOnPasteTerm()
    {
        return onPasteTerm;
    }

    public void setOnPasteTerm(boolean onPasteTerm)
    {
        this.onPasteTerm = onPasteTerm;
    }

    public boolean isOnPastePronounciation()
    {
        return onPastePronounciation;
    }

    public void setOnPastePronounciation(boolean onPastePronounciation)
    {
        this.onPastePronounciation = onPastePronounciation;
    }

    public boolean isOnPasteAcronym()
    {
        return onPasteAcronym;
    }

    public void setOnPasteAcronym(boolean onPasteAcronym)
    {
        this.onPasteAcronym = onPasteAcronym;
    }

    public boolean isOnPastePhraseology()
    {
        return onPastePhraseology;
    }

    public void setOnPastePhraseology(boolean onPastePhraseology)
    {
        this.onPastePhraseology = onPastePhraseology;
    }
}
