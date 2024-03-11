package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import de.l3s.util.Deletable;
import de.l3s.util.HasId;

/**
 * A predefined answer that a user can select for a SurveyQuestion.
 */
public class SurveyQuestionOption implements HasId, Deletable, Serializable {
    @Serial
    private static final long serialVersionUID = -6330747546265218917L;

    private int id;
    private int questionId;
    private boolean deleted;
    private String value;

    public SurveyQuestionOption() {
    }

    public SurveyQuestionOption(String value) {
        this.value = value;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(final int optionId) {
        this.id = optionId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(final int questionId) {
        this.questionId = questionId;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SurveyQuestionOption that = (SurveyQuestionOption) o;
        return id == that.id && questionId == that.questionId && deleted == that.deleted && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, questionId, deleted, value);
    }
}
