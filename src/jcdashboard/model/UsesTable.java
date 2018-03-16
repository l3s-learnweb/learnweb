package jcdashboard.model;

public class UsesTable
{

    String userid;
    Integer total;
    Integer pronounciation;
    Integer acronym;
    Integer phraseology;
    Integer uses;
    Integer source;

    public String getUserid()
    {
        return userid;
    }

    public void setUserid(String userid)
    {
        this.userid = userid;
    }

    public Integer getTotal()
    {
        return total;
    }

    public void setTotal(Integer total)
    {
        this.total = total;
    }

    public Integer getPronounciation()
    {
        return pronounciation;
    }

    public void setPronounciation(Integer pronounciation)
    {
        this.pronounciation = pronounciation;
    }

    public Integer getAcronym()
    {
        return acronym;
    }

    public void setAcronym(Integer acronym)
    {
        this.acronym = acronym;
    }

    public Integer getPhraseology()
    {
        return phraseology;
    }

    public void setPhraseology(Integer phraseology)
    {
        this.phraseology = phraseology;
    }

    public Integer getUses()
    {
        return uses;
    }

    public void setUses(Integer uses)
    {
        this.uses = uses;
    }

    public Integer getSource()
    {
        return source;
    }

    public void setSource(Integer source)
    {
        this.source = source;
    }

    public float getAvg()
    {
        return ((float) (pronounciation + acronym + phraseology + uses + source) / (total * 5));
    }

    @Override
    public String toString()
    {
        return "UsesTable [userid=" + userid + ", total=" + total + ", pronounciation=" + pronounciation + ", acronym=" + acronym + ", phraseology=" + phraseology + ", uses=" + uses + ", source=" + source + "]";
    }

}
