package de.l3s.learnweb.resource.glossaryNew;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.Size;

import org.jsoup.helper.Validate;

public class GlossaryEntry implements Serializable
{
    private static final long serialVersionUID = 1251808024273639912L;

    private int id = -1;
    private int originalEntryId = 0;
    private int resourceId;
    private boolean deleted;
    private int userId; // the user who created this entry
    private int lastChangedByUserId;
    private Timestamp timestamp;
    private String description;
    private boolean descriptionPasted = false;
    @Size(max = 100)
    private String topicOne;
    private String topicTwo;
    private String topicThree;
    private List<GlossaryTerm> terms = new LinkedList<>();

    /**
     * do nothing constructor
     */
    public GlossaryEntry()
    {

    }

    /**
     * copy constructor
     *
     * @param oldEntry
     */
    public GlossaryEntry(GlossaryEntry oldEntry)
    {
        setId(-1);
        setOriginalEntryId(oldEntry.id);
        setResourceId(oldEntry.resourceId);
        setDeleted(oldEntry.deleted);
        setUserId(oldEntry.userId);
        setLastChangedByUserId(oldEntry.lastChangedByUserId);
        setDescription(oldEntry.description);
        setDescriptionPasted(oldEntry.descriptionPasted);
        setTopicOne(oldEntry.topicOne);
        setTopicTwo(oldEntry.topicTwo);
        setTopicThree(oldEntry.topicThree);

        for(GlossaryTerm oldTerm : oldEntry.terms)
        {
            this.addTerm(oldTerm.clone());
        }
    }

    @Override
    public GlossaryEntry clone()
    {
        return new GlossaryEntry(this);
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getTopicOne()
    {
        return topicOne;
    }

    public void setTopicOne(String topicOne)
    {
        this.topicOne = topicOne;
    }

    public String getTopicTwo()
    {
        return topicTwo;
    }

    public void setTopicTwo(String topicTwo)
    {
        this.topicTwo = topicTwo;
    }

    public String getTopicThree()
    {
        return topicThree;
    }

    public void setTopicThree(String topicThree)
    {
        this.topicThree = topicThree;
    }

    public boolean isDescriptionPasted()
    {
        return descriptionPasted;
    }

    public void setDescriptionPasted(boolean onPasteDescription)
    {
        this.descriptionPasted = onPasteDescription;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;

        // update all terms too
        getTerms().forEach(term -> term.setEntryId(id));
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public List<GlossaryTerm> getTerms()
    {
        return terms;
    }

    void setTerms(List<GlossaryTerm> terms)
    {
        Validate.notNull(terms);

        this.terms = terms;
    }

    public void addTerm(GlossaryTerm term)
    {
        this.terms.add(term);
    }

    public int getLastChangedByUserId()
    {
        return lastChangedByUserId;
    }

    public void setLastChangedByUserId(int lastChangedByUserId)
    {
        this.lastChangedByUserId = lastChangedByUserId;
    }

    public int getOriginalEntryId()
    {
        return originalEntryId;
    }

    public void setOriginalEntryId(int originalEntryId)
    {
        this.originalEntryId = originalEntryId;
    }

    public Timestamp getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp)
    {
        this.timestamp = timestamp;
    }

}
