package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.resource.survey.SurveyManager;

@Named
@ViewScoped
public class EUMade4AllStatisticsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5340388635921946783L;
    private final LinkedList<Row> table = new LinkedList<>();
    private int evaluationFormAnswer;
    private int aarhusSurveyAnswer;

    public EUMade4AllStatisticsBean() throws SQLException
    {
        if(getUser() == null)
            return;
        if(!getUser().isModerator())
        {
            addAccessDeniedMessage();
            return;
        }
        /*
        205861  107     EU-Aarhus,Public
        213674  30      EU-Florence
        204489  31      EU-Leeds
        213673  24      EU-Messina
        204678  82      EU-Rome
        204700  6       EU-Rome
        */

        /*
         SELECT resource_id , count(user_id), group_concat(distinct title) FROM `lw_survey_resource`
        join lw_survey_resource_user using(resource_id)
        join lw_user_course using(user_id)
        join lw_course using (course_id)
        WHERE `survey_id` = 18
        group by resource_id
        ORDER BY group_concat(distinct title) ASC
         */
        Row baselineSurvey = new Row();
        baselineSurvey.setAarhus(new Link("107", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 205861));
        baselineSurvey.setFlorence(new Link("30", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 213674));
        baselineSurvey.setLeeds(new Link("31", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 204489));
        baselineSurvey.setMessina(new Link("24", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 213673));
        baselineSurvey.setRome(new Link("82", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 204678));
        table.add(baselineSurvey);

        SurveyManager sm = getLearnweb().getSurveyManager();
        evaluationFormAnswer = sm.getSurveyResource(216012).getAnswersOfAllUsers().size();
        aarhusSurveyAnswer = sm.getSurveyResource(219453).getAnswersOfAllUsers().size();
    }

    public int getEvaluationFormAnswer()
    {
        return evaluationFormAnswer;
    }

    public int getAarhusSurveyAnswer()
    {
        return aarhusSurveyAnswer;
    }

    public List<Row> getTable()
    {
        return table;
    }

    public static class Row
    {
        private Link aarhus;
        private Link florence;
        private Link leeds;
        private Link messina;
        private Link rome;

        public Link getAarhus()
        {
            return aarhus;
        }

        public void setAarhus(Link aarhus)
        {
            this.aarhus = aarhus;
        }

        public Link getFlorence()
        {
            return florence;
        }

        public void setFlorence(Link florence)
        {
            this.florence = florence;
        }

        public Link getLeeds()
        {
            return leeds;
        }

        public void setLeeds(Link leeds)
        {
            this.leeds = leeds;
        }

        public Link getMessina()
        {
            return messina;
        }

        public void setMessina(Link messina)
        {
            this.messina = messina;
        }

        public Link getRome()
        {
            return rome;
        }

        public void setRome(Link rome)
        {
            this.rome = rome;
        }
    }

    public static class Link
    {
        private final String title;
        private final String link;

        public Link(String title, String link)
        {
            super();
            this.title = title;
            this.link = link;
        }

        public String getTitle()
        {
            return title;
        }

        public String getLink()
        {
            return link;
        }

    }
}
