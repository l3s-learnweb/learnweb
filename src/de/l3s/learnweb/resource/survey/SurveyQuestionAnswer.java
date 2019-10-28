package de.l3s.learnweb.resource.survey;

import java.io.Serializable;

/**
 *
 * A predefined answer that a user can select for a SurveyQuestion.
 *
 */
public class SurveyQuestionAnswer implements Serializable
{
    private static final long serialVersionUID = -6330747546265218917L;

    private String value;

    public SurveyQuestionAnswer()
    {

    }

    public SurveyQuestionAnswer(String value)
    {
        super();
        this.value = value;
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
}
