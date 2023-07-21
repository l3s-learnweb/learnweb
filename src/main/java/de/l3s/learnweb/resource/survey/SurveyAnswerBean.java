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

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDetailBean;

@Named
@ViewScoped
public class SurveyAnswerBean extends ApplicationBean implements Serializable {
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
                SurveyResponse response = surveyDao.findResponseByResourceAndUserId(k, getUser().getId());
                if (response != null) {
                    return response;
                }
            }

            SurveyResponse response = new SurveyResponse(k);
            if (isLoggedIn()) {
                response.setUserId(getUser().getId());
            }
            surveyDao.saveResponse(response);
            return response;
        });

        if (response.isSubmitted()) {
            formEnabled = false;
            addMessage(FacesMessage.SEVERITY_WARN, "survey_already_submitted");
        } else if (!resource.isValidDate()) {
            formEnabled = false;
            addMessage(FacesMessage.SEVERITY_ERROR, "survey_submit_error_between", resource.getOpenDate(), resource.getCloseDate());
        } else if (!response.getAnswers().isEmpty()) {
            addGrowl(FacesMessage.SEVERITY_WARN, "survey.unfinished_form_loaded");
        }
    }

    public void onSubmit() {
        if (response.isSubmitted()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "survey_already_submitted");
            log.error("Survey already submitted. Should not happen. User: {}; Survey: {}", getUser().getId(), resource.getId());
            return;
        }

        response.setSubmitted(true);
        surveyDao.saveResponse(response);
        addMessage(FacesMessage.SEVERITY_INFO, "survey_submitted");
        log(Action.survey_submit, resource.getGroupId(), resource.getId());
        formEnabled = false;
    }

    public void onQuestionAnswer(SurveyQuestion question) {
        int variant = 0;
        if (getPage().isSampling()) {
            variant = getPage().getVariant(response.getId()).getId();
        }
        surveyDao.saveAnswer(response, question.getId(), variant, response.getAnswers().get(question.getId()));
    }

    public SurveyPage getPage() {
        if (resource.getPages().isEmpty()) {
            return null;
        }

        return resource.getPages().get(page - 1);
    }

    public List<SurveyQuestion> getQuestions() {
        if (getPage() == null) {
            return null;
        }

        return getPage().getQuestions();
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
