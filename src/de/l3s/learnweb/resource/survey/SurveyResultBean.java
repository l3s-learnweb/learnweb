package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.util.LinkedList;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;

@Named
@ViewScoped
public class SurveyResultBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 706177879900332816L;
    //private static final Logger log = LogManager.getLogger(SurveyResultBean.class);

    private int surveyResourceId;
    private SurveyResource resource;
    private LinkedList<SurveyQuestion> questionColumns; // lists the questions that are shown in the table

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());
        BeanAssert.hasPermission(getUser().isModerator());

        resource = dao().getSurveyDao().findResourceById(surveyResourceId).orElse(null);
        BeanAssert.isFound(resource);
        BeanAssert.notDeleted(resource);

        // output only questions that are not readonly
        questionColumns = new LinkedList<>();
        for (SurveyQuestion question : getResource().getQuestions()) {
            if (question.getType().isReadonly()) {
                continue;
            }

            questionColumns.add(question);
        }
    }

    public int getSurveyResourceId() {
        return surveyResourceId;
    }

    public void setSurveyResourceId(int surveyResourceId) {
        this.surveyResourceId = surveyResourceId;
    }

    public SurveyResource getResource() {
        return resource;
    }

    public LinkedList<SurveyQuestion> getQuestionColumns() {
        return questionColumns;
    }
}
