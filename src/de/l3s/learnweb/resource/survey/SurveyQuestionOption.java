package de.l3s.learnweb.resource.survey;

import java.io.Serializable;

/**
 *
 * A predefined answer that a user can select for a SurveyQuestion.
 *
 */
public class SurveyQuestionOption implements Serializable
{
    private static final long serialVersionUID = -6330747546265218917L;

    private String value;
    private boolean deleted;
    private int id;

    public SurveyQuestionOption()
    {
        this("");
    }

    public SurveyQuestionOption(String value)
    {
        super();
        this.value = value;
    }

    public int getId()
    {
        return id;
    }

    public void setId(final int id)
    {
        this.id = id;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return value;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(final boolean deleted)
    {
        this.deleted = deleted;
    }
}
