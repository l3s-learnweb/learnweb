package de.l3s.learnweb.resource.glossaryNew;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

import javax.validation.constraints.Size;

import de.l3s.util.StringHelper;

public class GlossaryTerm implements Serializable
{
    private static final long serialVersionUID = -8309235925484416943L;

    private int id = -1;
    private boolean deleted;
    private int entryId;
    private int userId; // the user who created this term

    @Size(max = 100)
    private String term;
    private List<String> uses;
    @Size(max = 200)
    private String pronounciation;
    @Size(max = 100)
    private String acronym;
    private String source;
    @Size(max = 500)
    private String phraseology;
    private Locale language;
    private Timestamp timestamp;
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

    public int getEntryId()
    {
        return entryId;
    }

    public void setEntryId(int entryId)
    {
        this.entryId = entryId;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    public Timestamp getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getUsesDisplayLabel()
    {
        if(getUses() == null || getUses().isEmpty())
            return "Use";
        else
            return StringHelper.implode(getUses(), ",");
    }

}
