package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.util.ArrayList;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class CreateSurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -2130517411614243639L;
    @Deprecated
    private ArrayList<SurveyQuestion> questions;
    @Deprecated
    private String title;
    @Deprecated
    private String description;
    private Survey survey;

    public CreateSurveyBean()
    {
        questions = new ArrayList<>();

        survey = new Survey(); // you can directly edit this survy. no need to redefine title desc and so on
        survey.setOrganizationId(getUser().getOrganisationId());
    }

    public String getTitle() // TODO use
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

    public void addQuestion(SurveyQuestion question)
    {
        questions.add(question);
    }

    public void removeQuestion(SurveyQuestion question)
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
