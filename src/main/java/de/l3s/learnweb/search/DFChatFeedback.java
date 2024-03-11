package de.l3s.learnweb.search;

import java.io.Serial;
import java.io.Serializable;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.survey.SurveyAnswerHandler;
import de.l3s.learnweb.resource.survey.SurveyDao;
import de.l3s.learnweb.resource.survey.SurveyPage;
import de.l3s.learnweb.resource.survey.SurveyQuestion;
import de.l3s.learnweb.resource.survey.SurveyResponse;

@Named("dfChatFeedback")
@ViewScoped
public class DFChatFeedback extends ApplicationBean implements Serializable, SurveyAnswerHandler {
    @Serial
    private static final long serialVersionUID = -2465620983158193954L;

    private static final Cache<Integer, SurveyResponse> responseCache = Caffeine.newBuilder().maximumSize(100).build();

    // params
    private Integer surveyId;
    private Integer messageId;

    private transient boolean readOnly = false;
    private transient SurveyPage survey;
    private transient SurveyResponse response;

    @Inject
    private SurveyDao surveyDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());
        survey = surveyDao.findPageById(surveyId).orElseThrow();

        response = responseCache.get(messageId, key -> {
            SurveyResponse response = surveyDao.findResponseByMessageId(key);
            if (response != null) {
                return response;
            }

            response = new SurveyResponse();
            response.setUserId(getUser().getId());
            response.setMessageId(key);
            return response;
        });

        if (getUser().getId() != response.getUserId()) {
            readOnly = true;
        }
    }

    public void onQuestionAnswer(SurveyQuestion question) {
        if (response.getId() == 0) {
            surveyDao.saveResponse(response);
        }

        int variant = 0;
        if (survey.isSampling()) {
            variant = survey.getVariant(response.getId()).getId();
        }

        surveyDao.saveAnswer(response, question, variant);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public Integer getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(final Integer surveyId) {
        this.surveyId = surveyId;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(final Integer messageId) {
        this.messageId = messageId;
    }

    public SurveyPage getSurvey() {
        return survey;
    }

    public SurveyResponse getResponse() {
        return response;
    }
}
