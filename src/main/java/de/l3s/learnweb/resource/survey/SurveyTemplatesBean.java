package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;

@Named
@ViewScoped
public class SurveyTemplatesBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 669287762248912801L;

    private Survey selectedSurvey;
    private List<Survey> surveys = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (!isLoggedIn()) {
            return;
        }

        this.surveys = dao().getSurveyDao().findByOrganisationIdOrUserId(getUser().getOrganisationId(), getUser().getId());
    }

    public Survey getSelectedSurvey() {
        return selectedSurvey;
    }

    public void setSelectedSurvey(final Survey selectedSurvey) {
        this.selectedSurvey = selectedSurvey;
    }

    /**
     * @return All surveys of the users organisation
     */
    public List<Survey> getSurveys() {
        return surveys;
    }

    public void onCopySurvey(int surveyId) {
        selectedSurvey = new Survey(dao().getSurveyDao().findById(surveyId).orElseThrow(BeanAssert.NOT_FOUND));
        selectedSurvey.setUserId(getUser().getId());
    }

    public void onCreateSurvey() {
        selectedSurvey = new Survey();
        selectedSurvey.setOrganisationId(getUser().getOrganisationId());
        selectedSurvey.setUserId(getUser().getId());
        selectedSurvey.setPublicTemplate(true);
    }

    /**
     * Saves the selectedSurvey and redirects to edit page.
     */
    public String onSave() {
        selectedSurvey.save(false);
        return "/lw/survey/template.xhtml?survey_id=" + selectedSurvey.getId() + "&faces-redirect=true";
    }

    public String onEditSurvey(int surveyId) {
        return "/lw/survey/template.xhtml?survey_id=" + surveyId + "&faces-redirect=true";
    }

    public void onDeleteSurvey(Survey surveyToDelete) {
        dao().getSurveyDao().deleteSoft(surveyToDelete);
        surveys.removeIf(survey -> survey.getId() == surveyToDelete.getId());
    }

}