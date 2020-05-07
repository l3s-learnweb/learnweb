package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import de.l3s.util.StringHelper;

public class GlossaryTerm implements Serializable, Cloneable
{
    private static final long serialVersionUID = -8309235925484416943L;

    private int id = -1;
    private int originalTermId = 0;
    private boolean deleted;
    private int entryId;
    private int userId; // the user who created this term
    private int lastChangedByUserId;
    @Length(max = 100)
    private String term;
    private List<String> uses;
    @Length(max = 200)
    private String pronounciation;
    @Length(max = 100)
    private String acronym;
    private String source;
    @Length(max = 1500)
    private String phraseology;
    private Locale language;
    private Timestamp timestamp;
    private boolean termPasted = false;
    private boolean pronounciationPasted = false;
    private boolean acronymPasted = false;
    private boolean phraseologyPasted = false;

    /**
     * Convenience function that calls the getter of a given field
     *
     * @param fieldName
     * @return
     */
    public String get(String fieldName)
    {
        switch(fieldName)
        {
        case "term":
            return getTerm();
        case "pronounciation":
            return getPronounciation();
        case "acronym":
            return getAcronym();
        case "source":
            return getSource();
        case "phraseology":
            return getPhraseology();
        default:
            throw new IllegalArgumentException(fieldName + " is not implemented");
        }
    }

    /**
     * do nothing constructor
     */
    public GlossaryTerm()
    {

    }

    public GlossaryTerm(GlossaryTerm oldTerm)
    {
        setId(-1);
        setOriginalTermId(oldTerm.id);
        setDeleted(oldTerm.deleted);
        setEntryId(oldTerm.entryId);
        setUserId(oldTerm.userId);
        setTerm(oldTerm.term);
        setLastChangedByUserId(oldTerm.lastChangedByUserId);
        setPronounciation(oldTerm.pronounciation);
        setAcronym(oldTerm.acronym);
        setSource(oldTerm.source);
        setPhraseology(oldTerm.phraseology);
        setLanguage(oldTerm.language);
        setTimestamp(new Timestamp(System.currentTimeMillis()));
        setTermPasted(oldTerm.termPasted);
        setPronounciationPasted(oldTerm.pronounciationPasted);
        setAcronymPasted(oldTerm.acronymPasted);
        setPhraseologyPasted(oldTerm.phraseologyPasted);

        setUses(new ArrayList<>(oldTerm.uses.size()));
        for(int i = 0, len = oldTerm.uses.size(); i < len; i++)
        {
            this.uses.add(i, oldTerm.uses.get(i));
        }

        // TODO since Strings are immutable this should be sufficient do clone the list. please test: setUses(new ArrayList<String>(oldTerm.uses));
    }

    @Override
    public GlossaryTerm clone()
    {
        return new GlossaryTerm(this);
    }

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
        if(!StringUtils.isEmpty(term))
            this.termPasted = termPasted;
    }

    public boolean isPronounciationPasted()
    {
        return pronounciationPasted;
    }

    public void setPronounciationPasted(boolean pronounciationPasted)
    {
        if(!StringUtils.isEmpty(pronounciation))
            this.pronounciationPasted = pronounciationPasted;
    }

    public boolean isAcronymPasted()
    {
        return acronymPasted;
    }

    public void setAcronymPasted(boolean acronymPasted)
    {
        if(!StringUtils.isEmpty(acronym))
            this.acronymPasted = acronymPasted;
    }

    public boolean isPhraseologyPasted()
    {
        return phraseologyPasted;
    }

    public void setPhraseologyPasted(boolean phraseologyPasted)
    {
        if(!StringUtils.isEmpty(phraseology))
            this.phraseologyPasted = phraseologyPasted;
    }

    public int getUserId()
    {
        return userId;
    }

    public int getLastChangedByUserId()
    {
        return lastChangedByUserId;
    }

    public void setLastChangedByUserId(int lastChangedByUserId)
    {
        this.lastChangedByUserId = lastChangedByUserId;
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
            return StringHelper.implode(getUses(), ", ");
    }

    public int getOriginalTermId()
    {
        return originalTermId;
    }

    public void setOriginalTermId(int originalTermId)
    {
        this.originalTermId = originalTermId;
    }
}
