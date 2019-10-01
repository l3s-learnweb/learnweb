package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.l3s.util.StringHelper;

public class GlossaryTableView implements Serializable
{
    private static final long serialVersionUID = -757320545292668593L;
    private GlossaryEntry entry;
    private GlossaryTerm term;

    public GlossaryTableView()
    {
        // required by Serializable
    }

    public GlossaryTableView(GlossaryEntry entry, GlossaryTerm term)
    {
        super();
        this.entry = entry;
        this.term = term;
    }

    public int getEntryId()
    {
        return entry.getId();
    }

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

    public Locale getLanguage()
    {
        return term.getLanguage();
    }

    public String getLanguageTag()
    {
        return term.getLanguage().toLanguageTag();
    }

    public String getUses()
    {
        return StringUtils.join(term.getUses(), ", ");
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

    public Timestamp getTimestamp()
    {
        return entry.getTimestamp();
    }

    public GlossaryEntry getEntry()
    {
        return entry;
    }

    public String getFulltext()
    {
        return entry.getFulltext();
    }

    public String getTopics()
    {
        StringBuilder sb = new StringBuilder(getEntry().getTopicOne());

        if(StringUtils.isNotBlank(getEntry().getTopicTwo()))
        {
            sb.append(" - ");
            sb.append(getEntry().getTopicTwo());
        }

        if(StringUtils.isNotBlank(getEntry().getTopicThree()))
        {
            sb.append(" - ");
            sb.append(getEntry().getTopicThree());
        }

        return StringHelper.shortnString(sb.toString(), 20);
    }
}
