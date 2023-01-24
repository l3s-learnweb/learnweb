package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;

@Named
@ViewScoped
public class SurveyViewBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -6217166153267996666L;
    private static final Logger log = LogManager.getLogger(SurveyViewBean.class);

    // query params
    private int responseId;

    private SurveyResource resource;
    private SurveyResponse response;
    private boolean formEnabled;

    @Inject
    private SurveyDao surveyDao;

    @PostConstruct
    public void onLoad() {
        response = surveyDao.findResponseById(responseId);
        resource = response.getResource();
        BeanAssert.hasPermission(response.getUserId() == getUser().getId() || resource.canModerateResource(getUser()));

        formEnabled = !response.isSubmitted() && response.getUserId() == getUser().getId() && resource.isValidDate();
    }

    public int getResponseId() {
        return responseId;
    }

    public void setResponseId(final int responseId) {
        this.responseId = responseId;
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
