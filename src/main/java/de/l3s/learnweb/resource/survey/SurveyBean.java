package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;

@Named
@ViewScoped
public class SurveyBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -6217166153267996666L;
    private static final Logger log = LogManager.getLogger(SurveyBean.class);
    private int surveyResourceId;
    private int surveyUserId; // the user whose answers are viewed, by default the currently logged in user

    private boolean formEnabled;

    private SurveyResource resource;
    private SurveyUserAnswers userAnswers;

    private String goBackPage; // contains a link the user should go to after filling in a survey
    private String goBackPageLink; // link to the go back page derived from goBackPage name
    private String goBackPageTitle; // title of the go back link

    @Inject
    private SurveyDao surveyDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        resource = surveyDao.findResourceById(surveyResourceId).orElseThrow(BeanAssert.NOT_FOUND);
        BeanAssert.notDeleted(resource);
        BeanAssert.hasPermission(resource.canViewResource(getUser()));

        // whose answers shall be viewed
        if (surveyUserId == 0 || surveyUserId == getUser().getId()) {
            // by default view the answers of the current user
            surveyUserId = getUser().getId();
        } else {
            // if a user wants to see the answers of another user, make sure he is a moderator
            BeanAssert.hasPermission(resource.canModerateResource(getUser()), "You are not allowed to view the answers of the given user");
        }

        userAnswers = resource.getAnswersOfUser(surveyUserId);

        formEnabled = !userAnswers.isSubmitted() && surveyUserId == getUser().getId() && isValidSubmissionDate(resource);
    }

    /**
     * This method is only called by the survey/survey.jsf page to load necessary warnings.
     */
    public void onLoadEdit() {
        if (null == resource) { // access violation detected in onLoad()
            return;
        }

        if (isSubmitted()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "survey_already_submitted");
        } else if (!isValidSubmissionDate(resource)) {
            addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_error_between", resource.getStart(), resource.getEnd());
        } else if (userAnswers.isSaved()) {
            if (resource.getEnd() != null) {
                addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_edit_until", resource.getEnd());
            } else {
                addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_edit");
            }
        }
    }

    public boolean isSubmitted() {
        if (null == userAnswers) {
            log.warn("userAnswers is null");
            return false;
        }
        return userAnswers.isSubmitted();
    }

    /**
     * @param submit true if final submit
     * @return false on error
     */
    private boolean onSaveOrSubmit(boolean submit) {
        if (isSubmitted()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "This Survey has already been submitted");
            log.error("Survey already submitted. Should not happen. User: {}; Survey: {}", surveyUserId, surveyResourceId);
            return false;
        }

        surveyDao.saveAnswers(userAnswers, submit);
        return true;
    }

    public void onSubmit() {
        if (onSaveOrSubmit(true)) {
            addMessage(FacesMessage.SEVERITY_INFO, "survey_submitted");
            log(Action.survey_submit, resource.getGroupId(), surveyResourceId);
            formEnabled = false;
        }
    }

    public void onSave() {
        if (onSaveOrSubmit(false)) {
            addMessage(FacesMessage.SEVERITY_INFO, "survey_saved");
            log(Action.survey_save, resource.getGroupId(), surveyResourceId);
        }
    }

    public int getSurveyResourceId() {
        return surveyResourceId;
    }

    public void setSurveyResourceId(int surveyResourceId) {
        this.surveyResourceId = surveyResourceId;
    }

    public int getSurveyUserId() {
        return surveyUserId;
    }

    public void setSurveyUserId(int surveyUserId) {
        this.surveyUserId = surveyUserId;
    }

    public SurveyResource getResource() {
        return resource;
    }

    public SurveyUserAnswers getUserAnswers() {
        return userAnswers;
    }

    public boolean isFormEnabled() {
        return formEnabled;
    }

    public String getGoBackPage() {
        return goBackPage;
    }

    public void setGoBackPage(String goBackPage) {
        this.goBackPage = goBackPage;

        switch (goBackPage) {
            case "assessmentResults" -> {
                goBackPageLink = "myhome/assessmentResults.jsf";
                goBackPageTitle = "Go back to the assessment results";
            }
            default -> log.error("Unknown value of goBackPage {}", goBackPage);
        }
    }

    public String getGoBackPageTitle() {
        return goBackPageTitle;
    }

    public String getGoBackPageLink() {
        return goBackPageLink;
    }

    private static boolean isValidSubmissionDate(SurveyResource surveyResource) {
        LocalDateTime currentDate = LocalDateTime.now();

        if (surveyResource.getStart() != null && surveyResource.getStart().isAfter(currentDate)) {
            return false;
        }
        if (surveyResource.getEnd() != null && surveyResource.getEnd().isBefore(currentDate)) {
            return false;
        }
        return true;
    }

}