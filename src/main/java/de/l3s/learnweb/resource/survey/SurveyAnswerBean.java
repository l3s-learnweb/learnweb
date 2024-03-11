package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;

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
    private int page = 1;
    private boolean formEnabled = true;

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
    }

    public SurveyPage getPage() {
        if (resource.getPages().isEmpty()) {
            return null;
        }

        return resource.getPages().get(page - 1);
    }

    public void changePage(int diff) {
        page += diff;
        if (page < 1) {
            page = 1;
        } else if (page > resource.getPages().size()) {
            page = resource.getPages().size();
        }
    }

    public int getCurrentPage() {
        return page;
    }

    public int getTotalPages() {
        return resource.getPages().size();
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
