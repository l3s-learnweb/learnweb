package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDetailBean;

@Named
@ViewScoped
public class SurveyResponsesBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 7539789403718546669L;
    private static final Logger log = LogManager.getLogger(SurveyResponsesBean.class);

    private SurveyResource resource;
    private transient LinkedList<SurveyQuestion> writeableQuestions;

    @Inject
    private SurveyDao surveyDao;

    @PostConstruct
    public void onLoad() {
        Resource baseResource = Beans.getInstance(ResourceDetailBean.class).getResource();
        resource = surveyDao.convertToSurveyResource(baseResource).orElseThrow(BeanAssert.NOT_FOUND);
        BeanAssert.hasPermission(resource.canModerateResource(getUser()));
    }

    /**
     * @return outputs only questions that are not readonly
     */
    public LinkedList<SurveyQuestion> getWriteableQuestions() {
        if (writeableQuestions == null) {
            writeableQuestions = new LinkedList<>();
            List<SurveyQuestion> questions = surveyDao.findQuestionsAndOptionsByResourceId(resource.getId());
            for (SurveyQuestion question : questions) {
                if (question.getType().isReadOnly()) {
                    continue;
                }

                writeableQuestions.add(question);
            }
        }
        return writeableQuestions;
    }

    /**
     * Returns all answers of user even when they are incomplete or not final.
     */
    public List<SurveyResponse> getAllResponses() {
        return surveyDao.findResponsesByResourceId(resource.getId());
    }

    /**
     * Returns only answers that were finally submitted.
     */
    public List<SurveyResponse> getSubmittedResponses() {
        return surveyDao.findSubmittedResponsesByResourceId(resource.getId());
    }
}
