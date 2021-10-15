package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.util.Deletable;
import de.l3s.util.HasId;

/**
 * This class contains only the questions of a survey.
 * An instance of this class may be used by many SurveyResource instances.
 *
 * @author Philipp Kemkes
 */
public class Survey implements Deletable, HasId, Serializable {
    @Serial
    private static final long serialVersionUID = -7478683722354893077L;

    private int id;
    @NotBlank
    @Size(min = 5, max = 100)
    private String title;
    @Size(max = 1000)
    private String description;
    private int organisationId; // if <> 0 only the specified organisation can use this survey
    private int userId; // user who created this survey
    private boolean deleted;
    private boolean publicTemplate = true; // if "true", other users in the organisation see it in the list of templates and can copy it
    private boolean associated; //'true' if survey associated with at least one resource

    private List<SurveyQuestion> questions;

    public Survey() {

    }

    public Survey(Survey old) {
        setId(0);
        setTitle(old.getTitle());
        setDescription(old.getDescription());
        setOrganisationId(old.getOrganisationId());
        setUserId(old.getUserId());
        setDeleted(old.isDeleted());
        setPublicTemplate(old.isPublicTemplate());

        for (SurveyQuestion question : old.getQuestions()) {
            questions.add(new SurveyQuestion(question));
        }
    }

    @Override
    public int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }

    public List<SurveyQuestion> getQuestions() {
        if (null == questions) {
            if (id == 0) {
                return new ArrayList<>();
            }

            questions = Learnweb.dao().getSurveyDao().findQuestionsAndAnswersById(id);
        }
        return questions;
    }

    public void setQuestions(List<SurveyQuestion> questions) {
        this.questions = questions;
    }

    public SurveyQuestion getQuestion(int questionId) {
        return getQuestions().stream().filter(q -> q.getId() == questionId).findFirst().orElseThrow(BeanAssert.NOT_FOUND);
    }

    /**
     * @param updateMetaDataOnly performance optimization: if true only metadata like title and description will be saved but not changes to questions
     */
    public void save(boolean updateMetaDataOnly) {
        Learnweb.dao().getSurveyDao().save(this, updateMetaDataOnly);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isPublicTemplate() {
        return publicTemplate;
    }

    public void setPublicTemplate(final boolean publicTemplate) {
        this.publicTemplate = publicTemplate;
    }

    public boolean isAssociated() {
        return associated;
    }

    public void setAssociated(final boolean associated) {
        this.associated = associated;
    }

}
