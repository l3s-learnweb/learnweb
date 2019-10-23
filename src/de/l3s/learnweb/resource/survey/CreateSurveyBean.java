package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.survey.SurveyQuestion.QuestionType;

@Named
@ViewScoped
public class CreateSurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -2130517411614243639L;

    private int surveyId;
    private Survey survey;

    public CreateSurveyBean() throws SQLException
    {
        // TODO move to onLoad()

        if(surveyId != 0) // edit an existing survey
            survey = getLearnweb().getSurveyManager().getSurvey(surveyId);
        else // create new survey
        {
            survey = new Survey();
            survey.setOrganizationId(getUser().getOrganisationId());
        }
    }

    public Survey getSurvey()
    {
        return survey;
    }

    public void onSave() throws SQLException
    {
        getLearnweb().getSurveyManager().save(survey);
    }

    public void onAddEmptyQuestion()
    {
        survey.addQuestion(new SurveyQuestion(QuestionType.INPUT_TEXT));
    }

    public void addEmptyAnswer(SurveyQuestion question)
    {
        question.getAnswers().add(new SurveyQuestionAnswer());
    }

}
