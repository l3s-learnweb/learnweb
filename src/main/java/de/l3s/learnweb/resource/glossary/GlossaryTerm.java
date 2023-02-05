package de.l3s.learnweb.resource.glossary;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import de.l3s.util.Deletable;
import de.l3s.util.HasId;

public class GlossaryTerm implements HasId, Deletable, Serializable {
    @Serial
    private static final long serialVersionUID = -8309235925484416943L;

    private int id;
    private int originalTermId;
    private boolean deleted;
    private int entryId;
    private int userId; // the user who created this term
    private int lastChangedByUserId;
    @Size(max = 100)
    private String term;
    private List<String> uses;
    @Size(max = 200)
    private String pronounciation;
    @Size(max = 100)
    private String acronym;
    private String source;
    @Size(max = 1500)
    private String phraseology;
    private Locale language;
    private boolean termPasted = false;
    private boolean pronounciationPasted = false;
    private boolean acronymPasted = false;
    private boolean phraseologyPasted = false;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    /**
     * do nothing constructor.
     */
    public GlossaryTerm() {

    }

    public GlossaryTerm(GlossaryTerm oldTerm) {
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
        setTermPasted(oldTerm.termPasted);
        setPronounciationPasted(oldTerm.pronounciationPasted);
        setAcronymPasted(oldTerm.acronymPasted);
        setPhraseologyPasted(oldTerm.phraseologyPasted);
        setUses(new ArrayList<>(oldTerm.uses));
    }

    /**
     * Convenience function that calls the getter of a given field.
     */
    public String getField(String fieldName) {
        return switch (fieldName) {
            case "term" -> getTerm();
            case "pronounciation" -> getPronounciation();
            case "acronym" -> getAcronym();
            case "source" -> getSource();
            case "phraseology" -> getPhraseology();
            default -> throw new IllegalArgumentException(fieldName + " is not implemented");
        };
    }

    /**
     * Convenience function that calls the setter of a given field.
     */
    public void setField(String fieldName, String toSet) {
        switch (fieldName) {
            case "term" -> setTerm(toSet);
            case "pronounciation" -> setPronounciation(toSet);
            case "acronym" -> setAcronym(toSet);
            case "source" -> setSource(toSet);
            case "phraseology" -> setPhraseology(toSet);
            default -> throw new IllegalArgumentException(fieldName + " is not implemented");
        }
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public List<String> getUses() {
        return uses;
    }

    public void setUses(List<String> uses) {
        Validate.notNull(uses);
        this.uses = uses.stream().map(String::trim).collect(Collectors.toList());
    }

    public String getPronounciation() {
        return pronounciation;
    }

    public void setPronounciation(String pronounciation) {
        this.pronounciation = pronounciation;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPhraseology() {
        return phraseology;
    }

    public void setPhraseology(String phraseology) {
        this.phraseology = phraseology;
    }

    public Locale getLanguage() {
        return language;
    }

    public void setLanguage(Locale language) {
        this.language = language;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isTermPasted() {
        return termPasted;
    }

    public void setTermPasted(boolean termPasted) {
        if (!StringUtils.isEmpty(term)) {
            this.termPasted = termPasted;
        }
    }

    public boolean isPronounciationPasted() {
        return pronounciationPasted;
    }

    public void setPronounciationPasted(boolean pronounciationPasted) {
        if (!StringUtils.isEmpty(pronounciation)) {
            this.pronounciationPasted = pronounciationPasted;
        }
    }

    public boolean isAcronymPasted() {
        return acronymPasted;
    }

    public void setAcronymPasted(boolean acronymPasted) {
        if (!StringUtils.isEmpty(acronym)) {
            this.acronymPasted = acronymPasted;
        }
    }

    public boolean isPhraseologyPasted() {
        return phraseologyPasted;
    }

    public void setPhraseologyPasted(boolean phraseologyPasted) {
        if (!StringUtils.isEmpty(phraseology)) {
            this.phraseologyPasted = phraseologyPasted;
        }
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getLastChangedByUserId() {
        return lastChangedByUserId;
    }

    public void setLastChangedByUserId(int lastChangedByUserId) {
        this.lastChangedByUserId = lastChangedByUserId;
    }

    public int getEntryId() {
        return entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsesDisplayLabel() {
        if (getUses() == null || getUses().isEmpty()) {
            return "Use";
        } else {
            return StringUtils.join(getUses(), ", ");
        }
    }

    public int getOriginalTermId() {
        return originalTermId;
    }

    public void setOriginalTermId(int originalTermId) {
        this.originalTermId = originalTermId;
    }
}
