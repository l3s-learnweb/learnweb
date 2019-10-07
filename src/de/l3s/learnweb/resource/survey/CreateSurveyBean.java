package de.l3s.learnweb.resource.survey;

import de.l3s.learnweb.beans.ApplicationBean;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Named
@ViewScoped
public class CreateSurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -2130517411614243639L;
    private ArrayList<SurveyQuestion> questions;
    private String title;
    private String description;

    public  CreateSurveyBean()
    {
        questions = new ArrayList<>();
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(final String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public void addQuestion (SurveyQuestion question)
    {
        questions.add(question);
    }

    public  void removeQuestion (SurveyQuestion question)
    {
        questions.remove(question);
    }

    public ArrayList<SurveyQuestion> getQuestions()
    {
        return questions;
    }

    public void setQuestions(final ArrayList<SurveyQuestion> questions)
    {
        this.questions = questions;
    }

    public void addEmptyQuestion()
    {
        questions.add(new SurveyQuestion());
    }

    public void createSurvey()
    {
        System.out.println(questions);
    }

}
