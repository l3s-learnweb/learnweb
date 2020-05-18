package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.HasId;

/**
 * This class contains only the questions of a survey.
 * An instance of this class may be used by many SurveyResource instances.
 *
 * @author Philipp
 */
public class Survey implements Serializable, HasId, Cloneable {
    private static final long serialVersionUID = -7478683722354893077L;

    private int id = -1;
    @NotBlank
    @Size(min = 5, max = 100)
    private String title;

    @Size(min = 0, max = 1000)
    private String description;
    private int organizationId; // if <> 0 only the specified organization can use this survey
    private int userId; // user who created this survey
    private boolean deleted;
    private boolean publicTemplate = true; // specifies whether the template is public (if "true", other users in the organization see it in the list of templates and can copy it)
    private boolean associated; //'true' if survey associated with at least one resource

    private List<SurveyQuestion> questions;

    public Survey() {

    }

    public Survey(Survey old) throws SQLException {
        setId(-1);
        setTitle(old.getTitle());
        setDescription(old.getDescription());
        setOrganizationId(old.getOrganizationId());
        setUserId(old.getUserId());
        setDeleted(old.isDeleted());
        setPublicTemplate(old.isPublicTemplate());

        for (SurveyQuestion question : old.getQuestions()) {
            questions.add(question.clone());
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

    public int getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
    }

    public List<SurveyQuestion> getQuestions() throws SQLException {
        if (null == questions) {
            questions = Learnweb.getInstance().getSurveyManager().getQuestions(id);
        }
        return questions;
    }

    public void setQuestions(List<SurveyQuestion> questions) {
        this.questions = questions;
    }

    public SurveyQuestion getQuestion(int questionId) throws SQLException {
        return getQuestions().stream().filter(q -> q.getId() == questionId).findFirst().get();
    }

    /**
     * @param updateMetadataOnly performance optimization: if true only metadata like title and description will be saved but not changes to questions
     */
    public void save(boolean updateMetaDataOnly) throws SQLException {
        Learnweb.getInstance().getSurveyManager().save(this, updateMetaDataOnly);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

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

    /**
     * Returns a copy of this Survey Template (Ids are set to default this the Object isn't persisted yet).
     */
    @Override
    public Survey clone() {
        try {
            return new Survey(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isAssociated() {
        return associated;
    }

    public void setAssociated(final boolean associated) {
        this.associated = associated;
    }

}
