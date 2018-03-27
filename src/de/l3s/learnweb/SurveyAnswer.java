package de.l3s.learnweb;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class SurveyAnswer implements Serializable
{
    private static final long serialVersionUID = -1442018853436353323L;

    // TODO none of these fields should be public, they have to be private
    public String userId; //  TODO why is the id stored as String????
    public LinkedHashMap<String, String> answers = new LinkedHashMap<String, String>();
    public String userName;
    public String studentId;

    public SurveyAnswer()
    {
    }

    public String getAnswer(String id)
    {
        if(answers.containsKey(id))
            return answers.get(id);
        else
            return "Unanswered";
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public LinkedHashMap<String, String> getAnswers()
    {
        return answers;
    }

    public void setAnswers(LinkedHashMap<String, String> answers)
    {
        this.answers = answers;
    }

    public String getStudentId()
    {
        return studentId;
    }

    public void setStudentId(String studentId)
    {
        this.studentId = studentId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

}
