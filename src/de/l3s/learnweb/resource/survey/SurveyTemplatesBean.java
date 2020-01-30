package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import org.primefaces.PrimeFaces;

@Named
@ViewScoped
public class SurveyTemplatesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 669287762248912801L;
    private final Survey survey;
    private Survey surveyCopy;
    private final List<Survey> surveys;
    private int associatedResourceId;

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

    public int getAssociatedResourceId()
    {
        return associatedResourceId;
    }

    public void setAssociatedResourceId(final int associatedResourceId)
    {
        this.associatedResourceId = associatedResourceId;
    }

    public void onCopySurvey() throws SQLException
    {
        surveyCopy.setUserId(getUser().getId());
        int copyId = getLearnweb().getSurveyManager().copySurvey(surveyCopy);

        UtilBean.redirect("/lw/survey/template.jsf?survey_id=" + copyId);
    }

    public void onEditSurvey(int surveyId) throws SQLException
    {
        SurveyResource associatedWithResource = getLearnweb().getSurveyManager().isSurveyAssociatedWithResource(surveyId);
        if(associatedWithResource != null)
        {
            this.associatedResourceId = associatedWithResource.getResourceId();
            PrimeFaces.current().executeScript("PF('templateAlreadyAssociatedDialog').show()");
        }
        else
        {
            UtilBean.redirect("/lw/survey/template.jsf?survey_id=" + surveyId);
        }
    }

    public void onDeleteSurvey(Survey survey) throws SQLException
    {
        if(getLearnweb().getSurveyManager().isSurveyAssociatedWithResource(survey.getId()) == null)
        {
            getLearnweb().getSurveyManager().deleteSurvey(survey.getId());
            surveys.remove(survey);
        }
        else
        {
            PrimeFaces.current().executeScript("PF('templateAlreadyAssociatedDialog').show()");
        }
    }

}
