package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class CreateSurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -2130517411614243639L;

    private Survey survey = new Survey();
    private static final AtomicInteger questionIdCount = new AtomicInteger(0);

    public CreateSurveyBean()
    {
    }

    public Survey getSurvey()
    {
        return survey;
    }

    public void setSurvey(final Survey survey)
    {
        this.survey = survey;
    }


    public void createSurvey() throws SQLException
    {
        try(PreparedStatement query = getLearnweb().getConnection().prepareStatement("INSERT INTO lw_survey (organization_id, title, description) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS))
        {
            query.setInt(1, getUser().getOrganisationId());
            query.setString(2, survey.getTitle());
            query.setString(3, survey.getDescription());
            query.executeUpdate();
            System.out.println(1);
            ResultSet rs = query.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            survey.setId(rs.getInt(1));
            createSurveyQuestions();
        }
    }

    private void createSurveyQuestions() throws SQLException
    {
        for (SurveyQuestion question : survey.getQuestions())
        {
            try(PreparedStatement query = getLearnweb().getConnection().prepareStatement("INSERT INTO `lw_survey_question`(`survey_id`, `question`, `question_type`) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS))
            {
                query.setInt(1, survey.getId());
                query.setString(2, question.getInfo());
                query.setString(3, question.getType().toString());
                query.executeUpdate();
                ResultSet rs = query.getGeneratedKeys();
                if(!rs.next())
                    throw new SQLException("database error: no id generated");
            }
        }
    }

    public void addEmptyQuestion()
    {
        survey.addQuestion(new SurveyQuestion(questionIdCount.incrementAndGet()));
    }

    public void addEmptyAnswer(int questionId)
    {
        survey.getQuestion(questionId).getAnswers().add(new SurveyQuestionAnswer());
    }

}
