package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class SurveyResultBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 706177879900332816L;
    //private static final Logger log = LogManager.getLogger(SurveyResultBean.class);

    private int surveyResourceId;
    private transient SurveyResource resource;
    private LinkedList<SurveyQuestion> questionColumns; // lists the questions that are shown in the table

    public void onLoad() throws SQLException
    {
        if(getUser() == null)
            return;

        if(!getUser().isModerator())
        {
            addAccessDeniedMessage();
            return;
        }

        if(getResource() == null)
        {
            addInvalidParameterMessage("resource_id");
            return;
        }

        // output only questions that are not readonly
        questionColumns = new LinkedList<>();
        for(SurveyQuestion question : resource.getQuestions())
        {
            if(question.getType().isReadonly())
                continue;

            questionColumns.add(question);
        }
    }

    public LinkedList<SurveyQuestion> getQuestionColumns()
    {
        return questionColumns;
    }

    public int getSurveyResourceId()
    {
        return surveyResourceId;
    }

    public void setSurveyResourceId(int surveyResourceId)
    {
        this.surveyResourceId = surveyResourceId;
    }

    public SurveyResource getResource() throws SQLException
    {
        if(resource == null)
        {
            resource = getLearnweb().getSurveyManager().getSurveyResource(surveyResourceId);
        }
        return resource;
    }
}
