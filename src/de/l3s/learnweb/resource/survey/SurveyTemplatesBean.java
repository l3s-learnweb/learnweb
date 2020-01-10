package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;

@Named
@ViewScoped
public class SurveyTemplatesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 669287762248912801L;
    private final Survey survey;
    private Survey surveyCopy;
    private final List<Survey> surveys;

    public SurveyTemplatesBean() throws SQLException
    {
        survey = new Survey();
        survey.setOrganizationId(getUser().getOrganisationId());
        survey.setUserId(getUser().getId());

        surveys = getLearnweb().getSurveyManager().getSurveysByOrganisation(getUser().getOrganisationId());
    }

    public void onCreateSurvey() throws SQLException
    {
        getLearnweb().getSurveyManager().save(survey);

        UtilBean.redirect("/lw/survey/template.jsf?survey_id=" + survey.getId());
    }

    /**
     *
     * @return All surveys of the users organisation
     * @throws SQLException
     */
    public List<Survey> getSurveys() throws SQLException
    {
        return surveys;
    }

    public Survey getSurvey()
    {
        return survey;
    }

    public Survey getSurveyCopy()
    {
        return surveyCopy;
    }

    public void setCopySurvey(Survey copySurvey)
    {
        this.surveyCopy = copySurvey;
    }

    public void onCopySurvey() throws SQLException
    {
        surveyCopy.setUserId(getUser().getId());
        int copyId = getLearnweb().getSurveyManager().copySurvey(surveyCopy);

        UtilBean.redirect("/lw/survey/template.jsf?survey_id=" + copyId);
    }
}
