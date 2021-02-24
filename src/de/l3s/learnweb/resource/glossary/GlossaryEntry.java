package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;
import org.jsoup.helper.Validate;

import de.l3s.util.Deletable;
import de.l3s.util.HasId;

public class GlossaryEntry implements HasId, Deletable, Serializable, Cloneable {
    private static final long serialVersionUID = 1251808024273639912L;

    private int id = -1;
    private Integer originalEntryId; // This value is -1 when the entry has been imported from a file. When a glossary resource is copied we save for each entry the id of the original entry from which it was copied.
    private int resourceId;
    private boolean deleted;
    private int userId; // the user who created this entry
    private Integer lastChangedByUserId;
    @Length(max = 900)
    private String description;
    private boolean descriptionPasted = false;
    @Length(max = 90)
    private String topicOne;
    @Length(max = 90)
    private String topicTwo;
    @Length(max = 90)
    private String topicThree;
    private List<GlossaryTerm> terms = new LinkedList<>();
    private String fulltext; //fulltext search in glossary
    private boolean imported;
    private LocalDateTime timestamp;

    /**
     * do nothing constructor.
     */
    public GlossaryEntry() {

    }

    /**
     * copy constructor.
     */
    public GlossaryEntry(GlossaryEntry oldEntry) {
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
        setFulltext(oldEntry.fulltext);
        setTimestamp(oldEntry.timestamp);

        for (GlossaryTerm oldTerm : oldEntry.terms) {
            this.addTerm(oldTerm.clone());
        }
    }

    @Override
    public GlossaryEntry clone() {
        return new GlossaryEntry(this);
    }

    private void generateFulltext() {
        StringBuilder text = new StringBuilder();
        //entry details
        text.append(topicOne);
        text.append(' ');
        text.append(topicTwo);
        text.append(' ');
        text.append(topicThree);
        text.append(' ');
        text.append(description);
        //term details
        for (GlossaryTerm term : terms) {
            if (term.isDeleted()) {
                continue;
            }

            text.append(' ');
            text.append(term.getTerm());
            text.append(' ');
            text.append(term.getAcronym());
            text.append(' ');
            text.append(term.getPronounciation());
            text.append(' ');
            text.append(term.getSource());
            text.append(' ');
            text.append(term.getPhraseology());
            text.append(' ');
            text.append(term.getUses());
        }
        fulltext = text.toString();

    }

    public GlossaryTerm getTerm(int termId) {
        for (GlossaryTerm term : terms) {
            if (term.getId() == termId) {
                return term;
            }
        }
        return null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTopicOne() {
        return topicOne;
    }

    public void setTopicOne(String topicOne) {
        this.topicOne = topicOne;
    }

    public String getTopicTwo() {
        return topicTwo;
    }

    public void setTopicTwo(String topicTwo) {
        this.topicTwo = topicTwo;
    }

    public String getTopicThree() {
        return topicThree;
    }

    public void setTopicThree(String topicThree) {
        this.topicThree = topicThree;
    }

    public boolean isDescriptionPasted() {
        return descriptionPasted;
    }

    public void setDescriptionPasted(boolean onPasteDescription) {
        if (!StringUtils.isEmpty(description)) {
            this.descriptionPasted = onPasteDescription;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;

        // update all terms too
        getTerms().forEach(term -> term.setEntryId(id));
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public List<GlossaryTerm> getTerms() {
        return terms;
    }

    void setTerms(List<GlossaryTerm> terms) {
        Validate.notNull(terms);

        this.terms = terms;
    }

    public void addTerm(GlossaryTerm term) {
        this.terms.add(term);
    }

    public Integer getLastChangedByUserId() {
        return lastChangedByUserId;
    }

    public void setLastChangedByUserId(Integer lastChangedByUserId) {
        this.lastChangedByUserId = lastChangedByUserId;
    }

    public Integer getOriginalEntryId() {
        return originalEntryId;
    }

    public void setOriginalEntryId(Integer originalEntryId) {
        this.originalEntryId = originalEntryId;
    }

    public String getFulltext() {
        if (fulltext == null) {
            generateFulltext();
        }
        return fulltext;
    }

    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(final boolean imported) {
        this.imported = imported;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Convenience function that calls the getter of a given field.
     */
    public String get(String fieldName) {
        switch (fieldName) {
            case "description":
                return getDescription();
            case "topicOne":
                return getTopicOne();
            case "topicTwo":
                return getTopicTwo();
            case "topicThree":
                return getTopicThree();
            case "fulltext":
                return getFulltext();
            default:
                throw new IllegalArgumentException(fieldName + " is not implemented");
        }
    }
}
