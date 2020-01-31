package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;

@Named
@ViewScoped
public class SurveyTemplatesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 669287762248912801L;

    @Deprecated
    private final Survey survey;

    @Deprecated
    private Survey surveyCopy;

    private Survey selectedSurvey;
    private final List<Survey> surveys;
    private int associatedResourceId;

    public SurveyTemplatesBean() throws SQLException
    {
        survey = new Survey();
        survey.setOrganizationId(getUser().getOrganisationId());
        survey.setUserId(getUser().getId());

        surveys = getLearnweb().getSurveyManager().getSurveysByOrganisation(getUser().getOrganisationId());
    }

    @Deprecated
    public void onCreateSurvey() throws SQLException
    {
        getLearnweb().getSurveyManager().save(survey);

        // TODO refactor
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

    @Deprecated
    public Survey getSurvey()
    {
        return survey;
    }

    @Deprecated
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

    @Deprecated
    public void setAssociatedResourceId(final int associatedResourceId)
    {
        this.associatedResourceId = associatedResourceId;
    }

    public void onCopySurveyNew() throws SQLException // called when button in table is pressed
    {
        selectedSurvey = getSurvey().clone();
        selectedSurvey.setUserId(getUser().getId());
    }

    public void onCreateSurveyNew() throws SQLException // called when button in table is pressed
    {
        selectedSurvey = new Survey();
        selectedSurvey.setOrganizationId(getUser().getOrganisationId());
        selectedSurvey.setUserId(getUser().getId());
    }

    /**
     * Saves the selectedSurvey and redirects to edit page
     *
     * @return
     * @throws SQLException
     */
    public String onSave() throws SQLException
    {
        selectedSurvey.save();

        return "/lw/survey/template.xhtml?survey_id=" + selectedSurvey.getId() + "&faces-redirect=true";

    }

    @Deprecated
    public void onCopySurvey() throws SQLException
    {
        // TODO refactor
        surveyCopy.setUserId(getUser().getId());
        int copyId = getLearnweb().getSurveyManager().copySurvey(surveyCopy);

        // TODO remove redirect, use JSF navigation https://www.tutorialspoint.com/jsf/jsf_page_navigation.htm
        UtilBean.redirect("/lw/survey/template.jsf?survey_id=" + copyId);
    }

    public void onEditSurvey(int surveyId) throws SQLException
    {
        SurveyResource associatedWithResource = getLearnweb().getSurveyManager().isSurveyAssociatedWithResource(surveyId);
        if(associatedWithResource != null)
        {
            this.associatedResourceId = associatedWithResource.getId();
            // TODO try to avoid using PrimeFaces.current . try using button onerror event or in this case disable the button and show add the tooltip to the button
            PrimeFaces.current().executeScript("PF('templateAlreadyAssociatedDialog').show()");
        }
        else

        {
            // TODO remove redirect, use JSF navigation https://www.tutorialspoint.com/jsf/jsf_page_navigation.htm
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
            // TODO remove
            PrimeFaces.current().executeScript("PF('templateAlreadyAssociatedDialog').show()");
        }
    }

}
