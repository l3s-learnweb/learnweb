package de.l3s.learnweb.beans.admin;

import java.io.Serial;
import java.io.Serializable;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.survey.SurveyDao;
import de.l3s.learnweb.resource.survey.SurveyPage;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.learnweb.user.Settings;

@Named
@ViewScoped
public class AdminChatFeedbackBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -4815509777068373043L;

    // params
    private int organisationId;

    private Organisation organisation;
    private SurveyPage promptSurvey;
    private SurveyPage responseSurvey;

    @Inject
    private OrganisationDao organisationDao;

    @Inject
    private SurveyDao surveyDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        if (organisationId != 0) {
            BeanAssert.hasPermission(getUser().isAdmin());
            organisation = organisationDao.findByIdOrElseThrow(organisationId);
        } else {
            BeanAssert.hasPermission(getUser().isModerator());
            organisation = getUser().getOrganisation(); // by default, edit the user's organisation
        }

        Integer promptSurveyId = organisation.getSettings().getIntValue(Settings.chat_feedback_prompt_survey_page_id);
        Integer responseSurveyId = organisation.getSettings().getIntValue(Settings.chat_feedback_response_survey_page_id);

        if (promptSurveyId != null) {
            promptSurvey = surveyDao.findPageById(promptSurveyId).orElseThrow();
        }

        if (responseSurveyId != null) {
            responseSurvey = surveyDao.findPageById(responseSurveyId).orElseThrow();
        }
    }

    public void createPromptSurvey() {
        promptSurvey = new SurveyPage();
        surveyDao.savePage(promptSurvey);

        organisation.getSettings().setValue(Settings.chat_feedback_prompt_survey_page_id, promptSurvey.getId());
        organisationDao.saveSettings(organisation.getId(), organisation.getSettings());
    }

    public void createResponseSurvey() {
        responseSurvey = new SurveyPage();
        surveyDao.savePage(responseSurvey);

        organisation.getSettings().setValue(Settings.chat_feedback_response_survey_page_id, responseSurvey.getId());
        organisationDao.saveSettings(organisation.getId(), organisation.getSettings());
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public SurveyPage getPromptSurvey() {
        return promptSurvey;
    }

    public SurveyPage getResponseSurvey() {
        return responseSurvey;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }
}
