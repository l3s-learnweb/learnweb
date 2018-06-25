package de.l3s.learnweb.resource.glossaryNew;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public class GlossaryTerm implements Serializable
{
    private static final long serialVersionUID = -8309235925484416943L;

    private int id;
    private int userId;

    private String term;
    private List<String> uses;
    private String pronounciation;
    private String acronym;
    private String source;
    private String phraseology;
    private Locale language;
    private boolean termPasted;
    private boolean pronounciationPasted;
    private boolean acronymPasted;
    private boolean phraseologyPasted;

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

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public boolean isTermPasted()
    {
        return termPasted;
    }

    public void setTermPasted(boolean termPasted)
    {
        this.termPasted = termPasted;
    }

    public boolean isPronounciationPasted()
    {
        return pronounciationPasted;
    }

    public void setPronounciationPasted(boolean pronounciationPasted)
    {
        this.pronounciationPasted = pronounciationPasted;
    }

    public boolean isAcronymPasted()
    {
        return acronymPasted;
    }

    public void setAcronymPasted(boolean acronymPasted)
    {
        this.acronymPasted = acronymPasted;
    }

    public boolean isPhraseologyPasted()
    {
        return phraseologyPasted;
    }

    public void setPhraseologyPasted(boolean phraseologyPasted)
    {
        this.phraseologyPasted = phraseologyPasted;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

}
