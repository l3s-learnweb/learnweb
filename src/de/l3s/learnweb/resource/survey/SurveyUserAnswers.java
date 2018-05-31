package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

/**
 * This class represents the answers of a single suer for a survey resources
 *
 * @author Philipp
 *
 */
public class SurveyUserAnswers implements Serializable, HasId
{
    private static final long serialVersionUID = -1442011853436353323L;

    private final int userId;
    private final int surveyResourceId;
    private final HashMap<Integer, String> answers = new LinkedHashMap<>(); // simple answers
    private final HashMap<Integer, String[]> multipleAnswers = new LinkedHashMap<>(); // used for questions that allow multiple answers
    private boolean saved; // has the user saved the survey at least once
    private boolean submitted; // has the user submitted the survey finally

    private transient User user;

    public SurveyUserAnswers(int userId, int surveyResourceId)
    {
        super();
        this.userId = userId;
        this.surveyResourceId = surveyResourceId;
    }

    public int getResourceId()
    {
        return surveyResourceId;
    }

    public String getAnswer(int id)
    {
        if(answers.containsKey(id))
            return answers.get(id);

        if(multipleAnswers.containsKey(id))
            return String.join(", ", multipleAnswers.get(id));

        return "Unanswered";
    }

    public int getUserId()
    {
        return userId;
    }

    public User getUser() throws SQLException
    {
        if(null == user)
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        return user;
    }

    public boolean isSaved()
    {
        return saved;
    }

    public void setSaved(boolean saved)
    {
        this.saved = saved;
    }

    public boolean isSubmitted()
    {
        return submitted;
    }

    public void setSubmitted(boolean submitted)
    {
        this.submitted = submitted;
    }

    public HashMap<Integer, String> getAnswers()
    {
        return answers;
    }

    public HashMap<Integer, String[]> getMultipleAnswers()
    {
        return multipleAnswers;
    }

    /**
     * This class should only be cached inside an Resource object hence it returns the userId as its id
     *
     * @return
     */
    @Override
    public int getId()
    {
        return getUserId();
    }

}
