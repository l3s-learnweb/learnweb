package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
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
 *
 */
public class Survey implements Serializable, HasId
{
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
    private boolean permissionToCopy;

    private List<SurveyQuestion> questions = new ArrayList<>();

    @Override
    public int getId()
    {
        return id;
    }

    protected void setId(int id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getOrganizationId()
    {
        return organizationId;
    }

    public void setOrganizationId(int organizationId)
    {
        this.organizationId = organizationId;
    }

    public List<SurveyQuestion> getQuestions()
    {
        return questions;
    }

    public void addQuestion(SurveyQuestion question)
    {
        questions.add(question);
    }

    public SurveyQuestion getQuestion(int questionId)
    {
        return questions.stream().filter(q -> q.getId() == questionId).findFirst().get();
    }

    public void save() throws SQLException
    {
        Learnweb.getInstance().getSurveyManager().save(this, true);
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(final boolean deleted)
    {
        this.deleted = deleted;
    }

    public boolean isPermissionToCopy()
    {
        return permissionToCopy;
    }

    public void setPermissionToCopy(final boolean permissionToCopy)
    {
        this.permissionToCopy = permissionToCopy;
    }

    /**
     * Returns a copy of this Survey Template (Ids are set to default this the Object isn't persisted yet).
     */
    @Override
    public Survey clone()
    {
        return null; // TODO implement
    }

}
