package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class SurveyTemplatesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 669287762248912801L;

    private Survey selectedSurvey;
    private List<Survey> surveys = new ArrayList<>();

    public SurveyTemplatesBean() throws SQLException
    {
        if(!isLoggedIn())
            return;

        this.surveys = getLearnweb().getSurveyManager().getPublicSurveysByOrganisationOrUser(getUser());
    }

    public Survey getSelectedSurvey()
    {
        return selectedSurvey;
    }

    public void setSelectedSurvey(final Survey selectedSurvey)
    {
        this.selectedSurvey = selectedSurvey;
    }

    /**
     *
     * @return All surveys of the users organisation
     */
    public List<Survey> getSurveys()
    {
        return surveys;
    }

    public void onCopySurvey(int surveyId) throws SQLException
    {
        selectedSurvey = getLearnweb().getSurveyManager().getSurvey(surveyId).clone();
        selectedSurvey.setUserId(getUser().getId());
    }

    public void onCreateSurvey()
    {
        selectedSurvey = new Survey();
        selectedSurvey.setOrganizationId(getUser().getOrganisationId());
        selectedSurvey.setUserId(getUser().getId());
        selectedSurvey.setPublicTemplate(true);
    }

    /**
     * Saves the selectedSurvey and redirects to edit page
     *
     * @return
     * @throws SQLException
     */
    public String onSave() throws SQLException
    {
        selectedSurvey.save(false);
        return "/lw/survey/template.xhtml?survey_id=" + selectedSurvey.getId() + "&faces-redirect=true";
    }

    public String onEditSurvey(int surveyId)
    {
        return "/lw/survey/template.xhtml?survey_id=" + surveyId + "&faces-redirect=true";
    }

    public void onDeleteSurvey(Survey surveyToDelete) throws SQLException
    {
        getLearnweb().getSurveyManager().deleteSurvey(surveyToDelete.getId());
        surveys.removeIf(survey -> survey.getId() == surveyToDelete.getId());
    }

}
