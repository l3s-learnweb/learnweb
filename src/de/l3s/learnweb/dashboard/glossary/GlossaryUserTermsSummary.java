package de.l3s.learnweb.dashboard.glossary;

import java.io.Serializable;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

public class GlossaryUserTermsSummary implements Serializable
{
    private static final long serialVersionUID = 8450045158991797739L;

    private int userId = -1;
    private int terms;
    private int termsPasted;
    private int pronounciation;
    private int pronounciationPasted;
    private int acronym;
    private int acronymPasted;
    private int phraseology;
    private int phraseologyPasted;
    private int uses;
    private int source;
    private int entries;

    private transient User user;

    public float getAvg()
    {
        return ((float) (pronounciation + acronym + phraseology + uses + source) / (terms * 5));
    }

    public float getTotalPastedPct()
    {
        long totalFields = pronounciation + acronym + phraseology + terms;
        long pastedFields = pronounciationPasted + acronymPasted + phraseologyPasted + termsPasted;
        return ((float) pastedFields / totalFields);
    }

    public User getUser() throws SQLException
    {
        if(null == user && userId > 0)
        {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public int getEntries()
    {
        return entries;
    }

    public void setEntries(int entries)
    {
        this.entries = entries;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(final int userId)
    {
        this.userId = userId;
    }

    public int getTerms()
    {
        return terms;
    }

    public void setTerms(final int total)
    {
        this.terms = total;
    }

    public int getTermsPasted()
    {
        return termsPasted;
    }

    public void setTermsPasted(final int termsPasted)
    {
        this.termsPasted = termsPasted;
    }

    public int getPronounciation()
    {
        return pronounciation;
    }

    public void setPronounciation(final int pronounciation)
    {
        this.pronounciation = pronounciation;
    }

    public int getPronounciationPasted()
    {
        return pronounciationPasted;
    }

    public void setPronounciationPasted(final int pronounciationPasted)
    {
        this.pronounciationPasted = pronounciationPasted;
    }

    public int getAcronym()
    {
        return acronym;
    }

    public void setAcronym(final int acronym)
    {
        this.acronym = acronym;
    }

    public int getAcronymPasted()
    {
        return acronymPasted;
    }

    public void setAcronymPasted(final int acronymPasted)
    {
        this.acronymPasted = acronymPasted;
    }

    public int getPhraseology()
    {
        return phraseology;
    }

    public void setPhraseology(final int phraseology)
    {
        this.phraseology = phraseology;
    }

    public int getPhraseologyPasted()
    {
        return phraseologyPasted;
    }

    public void setPhraseologyPasted(final int phraseologyPasted)
    {
        this.phraseologyPasted = phraseologyPasted;
    }

    public int getUses()
    {
        return uses;
    }

    public void setUses(final int uses)
    {
        this.uses = uses;
    }

    public int getSource()
    {
        return source;
    }

    public void setSource(final int source)
    {
        this.source = source;
    }
}
