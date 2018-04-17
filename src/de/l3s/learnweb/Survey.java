package de.l3s.learnweb;

import java.io.Serializable;
import java.util.ArrayList;

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
    private String title;
    private String description;
    private int organizationId; // if <> 0 only the specified organization can use this survey

    private ArrayList<SurveyMetaDataFields> questions = new ArrayList<SurveyMetaDataFields>();

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

    public ArrayList<SurveyMetaDataFields> getQuestions()
    {
        return questions;
    }
}
