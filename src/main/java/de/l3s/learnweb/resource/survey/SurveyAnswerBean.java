package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;
import org.primefaces.PrimeFaces;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDetailBean;
import de.l3s.util.bean.BeanHelper;

@Named
@ViewScoped
public class SurveyAnswerBean extends ApplicationBean implements Serializable, SurveyAnswerHandler {
    @Serial
    private static final long serialVersionUID = -6217166153267996666L;
    private static final Logger log = LogManager.getLogger(SurveyAnswerBean.class);

    private SurveyResource resource;
    private SurveyResponse response;
    private Integer currentPageId;
    private boolean formEnabled = true;

    private transient List<SurveyPage> visiblePages;

    @Inject
    private SurveyDao surveyDao;

    @PostConstruct
    public void onLoad() {
        Resource baseResource = Beans.getInstance(ResourceDetailBean.class).getResource();
        resource = surveyDao.convertToSurveyResource(baseResource).orElseThrow(BeanAssert.NOT_FOUND);
        BeanAssert.hasPermission(resource.canViewResource(getUser()));

        response = getUserBean().getSurveyResponses().computeIfAbsent(resource.getId(), k -> {
            if (isLoggedIn()) {
                SurveyResponse byUser = surveyDao.findResponseByResourceAndUserId(k, getUser().getId());
                if (byUser != null) {
                    return byUser;
                }
            }

            SurveyResponse newResponse = new SurveyResponse();
            newResponse.setResourceId(k);
            if (isLoggedIn()) {
                newResponse.setUserId(getUser().getId());
            }
            surveyDao.saveResponse(newResponse);
            return newResponse;
        });

        if (response.isSubmitted()) {
            formEnabled = false;
            addMessage(FacesMessage.SEVERITY_WARN, "survey.answer_already_submitted");
        } else if (!resource.isValidDate()) {
            formEnabled = false;
            addMessage(FacesMessage.SEVERITY_ERROR, "survey.answer_restricted_dates_between", BeanHelper.date(resource.getOpenDate()), BeanHelper.date(resource.getCloseDate()));
        } else if (!response.isEmpty()) {
            addGrowl(FacesMessage.SEVERITY_WARN, "survey.answer_unfinished_loaded");
        }
    }

    public void onDiscard() {
        surveyDao.deleteAnswersByResponseId(response.getId());
        response.clear();

        response.setSubmitted(false);
        surveyDao.saveResponse(response);

        addMessage(FacesMessage.SEVERITY_INFO, "survey.answers_discarded");
        formEnabled = true;
        currentPageId = null;
        visiblePages = null;
    }

    public void onSubmit() {
        if (response.isSubmitted()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "survey.answer_already_submitted");
            log.error("Survey already submitted. Should not happen. User: {}; Survey: {}", getUser().getId(), resource.getId());
            return;
        }

        response.setSubmitted(true);
        surveyDao.saveResponse(response);
        addMessage(FacesMessage.SEVERITY_INFO, "survey.answer_submitted");
        log(Action.survey_submit, resource.getGroupId(), resource.getId());
        formEnabled = false;
    }

    public void onQuestionAnswer(SurveyQuestion question) {
        int variant = 0;
        if (getPage().isSampling()) {
            variant = getPage().getVariant(response.getId()).getId();
        }
        surveyDao.saveAnswer(response, question, variant);

        if (question.isExposable()) {
            visiblePages = null; // clear cache to re-evaluate visibility
        }

        if (question.isExposable()) {
            PrimeFaces.current().ajax().update("survey_form:questions");
        }
    }

    public SurveyPage getPage() {
        if (getVisiblePages().isEmpty()) {
            return null;
        }

        return getVisiblePages().get(indexOfCurrent());
    }

    public void changePage(int diff) {
        int idx = indexOfCurrent() + diff;
        if (idx >= 0 && idx < getVisiblePages().size()) {
            currentPageId = getVisiblePages().get(idx).getId();
        }
    }

    private List<SurveyPage> getVisiblePages() {
        if (visiblePages == null) {
            visiblePages = resource.getPages().stream().filter(this::isPageVisible).toList();
        }

        return visiblePages;
    }

    private boolean isPageVisible(SurveyPage page) {
        if (!page.hasCondition()) {
            return true;
        }

        String answer = response.getAnswers().get(page.getRequiredQuestionId());
        if (answer == null) {
            return false;
        }

        return answer.equalsIgnoreCase(page.getRequiredAnswer());
    }

    public boolean isQuestionVisible(SurveyPage page, SurveyQuestion question) {
        if (!question.hasCondition()) {
            return true;
        }

        String answer = response.getAnswers().get(question.getRequiredQuestionId());
        if (answer == null) {
            return false;
        }

        return answer.equalsIgnoreCase(question.getRequiredAnswer());
    }

    private int indexOfCurrent() {
        if (currentPageId != null && currentPageId > 0) {
            for (int i = 0; i < getVisiblePages().size(); i++) {
                if (getVisiblePages().get(i).getId() == currentPageId) {
                    return i;
                }
            }
        }

        return 0;
    }

    public int getCurrentPage() {
        return indexOfCurrent() + 1;
    }

    public boolean isHasPreviousPage() {
        return indexOfCurrent() > 0;
    }

    public boolean isHasNextPage() {
        if (getVisiblePages().isEmpty() || getVisiblePages().size() == 1) {
            return false;
        }

        return indexOfCurrent() < getVisiblePages().size() - 1;
    }

    public int getTotalPages() {
        return getVisiblePages().size();
    }

    public SurveyResource getResource() {
        return resource;
    }

    public SurveyResponse getResponse() {
        return response;
    }

    public boolean isFormEnabled() {
        return formEnabled;
    }
}
