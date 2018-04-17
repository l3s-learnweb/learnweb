package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SurveyUserAnswers;

@ViewScoped
@ManagedBean
public class SurveyResultBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 706177879900332816L;
    private static final Logger log = Logger.getLogger(SurveyResultBean.class);
    private int resourceId;
    private String title;
    private List<SurveyUserAnswers> answers;
    private List<ColumnModel> columns;

    @PostConstruct
    public void init()
    {
        try
        {
            resourceId = getParameterInt("resource_id");

        }
        catch(NullPointerException e)
        {
            resourceId = 0;
        }

        if(resourceId > 0)
        {

            try
            {
                setTitle(getLearnweb().getResourceManager().getResource(resourceId).getTitle());
            }
            catch(SQLException e)
            {
                log.warn("Couldn't fetch survey result title for resource id: " + resourceId);
            }
            getSurveyResult();
        }

    }

    private void getSurveyResult()
    {
        columns = new ArrayList<ColumnModel>();
        try
        {

            LinkedHashMap<Integer, String> questions = getLearnweb().getSurveyManager().getAnsweredQuestions(resourceId);
            answers = getLearnweb().getSurveyManager().getAnswerOfAllUserForSurveyResource(getResourceId(), questions);
            int questionIndex = 1;
            for(Integer qid : questions.keySet())
            {
                ColumnModel col = new ColumnModel(questions.get(qid), qid, questionIndex);
                columns.add(col);
                questionIndex++;
            }
        }
        catch(Exception e)
        {
            log.error("Error in fetching result for survey: " + resourceId, e);
        }
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public List<SurveyUserAnswers> getAnswers()
    {
        return answers;
    }

    public List<ColumnModel> getColumns()
    {
        return columns;
    }

    static public class ColumnModel implements Serializable
    {
        private static final long serialVersionUID = -8787608049574883366L;
        private String header;
        private int id;
        private int index;

        public ColumnModel(String header, int id, int index)
        {
            this.header = header;
            this.id = id;
            this.index = index;
        }

        public String getHeader()
        {
            return header;
        }

        public int getId()
        {
            return id;
        }

        public int getIndex()
        {
            return index;
        }

        public void setIndex(int index)
        {
            this.index = index;
        }
    }
}
