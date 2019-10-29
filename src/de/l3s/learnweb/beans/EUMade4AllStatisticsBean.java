package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.peerAssessment.PeerAssessmentManager;
import de.l3s.learnweb.resource.peerAssessment.PeerAssessmentPair;
import de.l3s.learnweb.resource.survey.SurveyManager;
import de.l3s.learnweb.resource.survey.SurveyQuestion;
import de.l3s.learnweb.resource.survey.SurveyQuestion.QuestionType;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.resource.survey.SurveyUserAnswers;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class EUMade4AllStatisticsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5340388635921946783L;
    private static final Logger log = Logger.getLogger(EUMade4AllStatisticsBean.class);
    private final LinkedList<Row> table = new LinkedList<>();
    private int evaluationFormAnswer;
    private int aarhusSurveyAnswer;
    private LinkedList<UserStatistic> userStatistics;
    private LinkedList<SurveyQuestion> questionColumns;
    private int assessmentType; // use to filter output by assessment type

    public EUMade4AllStatisticsBean()
    {
    }

    public void onLoad() throws SQLException
    {/*
     if(getUser() == null)
      return;
     if(!getUser().isModerator())
     {
      addAccessDeniedMessage();
      return;
     }

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
        Row baselineSurveyRow = new Row();
        baselineSurveyRow.setAarhus(new Link("107", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 205861));
        baselineSurveyRow.setFlorence(new Link("30", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 213674));
        baselineSurveyRow.setLeeds(new Link("31", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 204489));
        baselineSurveyRow.setMessina(new Link("24", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 213673));
        baselineSurveyRow.setRome(new Link("82", "http://learnweb.l3s.uni-hannover.de/lw/survey/results.jsf?resource_id=" + 204678));
        table.add(baselineSurveyRow);

        SurveyManager sm = getLearnweb().getSurveyManager();

        // merge answers of baseline survey and index by userId
        int[] baselineSurveyIds = { 205861, 213674, 204489, 213673, 204678 };
        HashMap<Integer, SurveyResource> baselineSurveyAnswers = new HashMap<>();
        for(int surveyResourceId : baselineSurveyIds)
        {
            SurveyResource baselineSurveyResource = sm.getSurveyResource(surveyResourceId);

            for(SurveyUserAnswers answer : baselineSurveyResource.getAnswersOfAllUsers())
            {
                baselineSurveyAnswers.put(answer.getUserId(), baselineSurveyResource);
            }
        }

        SurveyResource evaluationSurvey = sm.getSurveyResource(216012);
        evaluationFormAnswer = evaluationSurvey.getAnswersOfAllUsers().size();
        aarhusSurveyAnswer = sm.getSurveyResource(219453).getAnswersOfAllUsers().size();

        userStatistics = new LinkedList<>();
        int[] peerAssessmentIds = { 1, 2, 3 };
        PeerAssessmentManager pam = getLearnweb().getPeerAssessmentManager();
        for(int peerAssessmentId : peerAssessmentIds)
        {
            List<PeerAssessmentPair> pairs = pam.getPairsByPeerAssessmentId(peerAssessmentId);

            for(PeerAssessmentPair pair : pairs)
            {
                if(assessmentType != 0 && assessmentType != pair.getPeerAssessment().getSurveyId())
                    continue;

                // check if the assessed user has submitted the baseline and evaluation surveys
                SurveyResource baselineSurvey = baselineSurveyAnswers.get(pair.getAssessedUserId());
                boolean evaluationAnswersSubmitted = evaluationSurvey.isSubmitted(pair.getAssessedUserId());
                userStatistics.add(new UserStatistic(pair, baselineSurvey == null ? -1 : baselineSurvey.getId(), evaluationAnswersSubmitted ? evaluationSurvey.getId() : -1));
            }
        }

        questionColumns = new LinkedList<>();

        // add baseline survey
        questionColumns.add(new SurveyQuestion(QuestionType.INPUT_TEXT, "Baseline"));

        for(SurveyQuestion question : sm.getSurveyResource(205861).getQuestions())
        {
            if(question.getType().isReadonly() || question.getLabel().equals("Name:") || question.getLabel().equals("Surname:"))
                continue;

            questionColumns.add(question);
        }

        // add evaluation survey
        questionColumns.add(new SurveyQuestion(QuestionType.INPUT_TEXT, "Evaluation"));

        for(SurveyQuestion question : evaluationSurvey.getQuestions())
        {
            if(question.getType().isReadonly() || question.getLabel().equals("Name of Your University:"))
                continue;

            questionColumns.add(question);
        }

        if(assessmentType != 0)
        {
            questionColumns.add(new SurveyQuestion(QuestionType.INPUT_TEXT, "Peer assessment"));

            for(SurveyQuestion question : getLearnweb().getSurveyManager().getSurvey(assessmentType).getQuestions())
            {
                if(question.getType().isReadonly() || question.getLabel().equals("Name of Your University:hj"))
                    continue;

                questionColumns.add(question);
            }

            /*
             15 25 corporate video x
            16      26 about
            19      27 fan video
            20      24 video mediated x
            21      23 weblogs x
             */

            int assessmentSurveyId;
            switch(assessmentType)
            {
            case 23:
                assessmentSurveyId = 21; //webblogs
                break;
            case 25:
                assessmentSurveyId = 15;// corporate video
                break;
            case 24:
                assessmentSurveyId = 20;// video medeiated itneractions
                break;
            case 27:
                assessmentSurveyId = 19;// fan video
                break;
            case 26:
                assessmentSurveyId = 16;// about
                break;
            default:
                addErrorMessage(new RuntimeException("invalid type " + assessmentType));
                return;
            }

            questionColumns.add(new SurveyQuestion(QuestionType.INPUT_TEXT, "Teacher's assessment"));

            for(SurveyQuestion question : getLearnweb().getSurveyManager().getSurvey(assessmentSurveyId).getQuestions())
            {
                if(question.getType().isReadonly() || question.getLabel().equals("Name of Your University:hj"))
                    continue;

                questionColumns.add(question);
            }
        }
    }

    public int getAssessmentType()
    {
        return assessmentType;
    }

    public String getAssessmentTypeName()
    {
        switch(assessmentType)
        {
        case 23:
            return "WEBLOGS";
        case 25:
            return "CORPORATE VIDEO";
        case 24:
            return "VIDEO MEDIATED INTERACTIONS";
        case 27:
            return "FAN VIDEO";
        case 26:
            return "ABOUT US PAGE";
        default:
            return "unknown";
        }
    }

    public void setAssessmentType(int assessmentType)
    {
        this.assessmentType = assessmentType;
    }

    public LinkedList<SurveyQuestion> getQuestionColumns()
    {
        return questionColumns;
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

    public LinkedList<UserStatistic> getUserStatistics()
    {
        return userStatistics;
    }

    public static class UserStatistic implements Serializable
    {
        private static final long serialVersionUID = -4241278453267455330L;

        private final PeerAssessmentPair peerAssessmentPair;
        private final int baselineSurveyResourceId;
        private final int evaluationSurveyResourceId;

        public UserStatistic(PeerAssessmentPair peerAssessmentPair, int baselineSurveyId, int evaluationSurveyId)
        {
            super();
            this.peerAssessmentPair = peerAssessmentPair;
            this.baselineSurveyResourceId = baselineSurveyId;
            this.evaluationSurveyResourceId = evaluationSurveyId;
        }

        public int getUserId()
        {
            return peerAssessmentPair.getAssessedUserId();
        }

        public User getUser() throws SQLException
        {
            return Learnweb.getInstance().getUserManager().getUser(getUserId());
        }

        public PeerAssessmentPair getPeerAssessmentPair()
        {
            return peerAssessmentPair;
        }

        /**
         * short convenience version
         *
         * @return
         */
        public PeerAssessmentPair getPair()
        {
            return peerAssessmentPair;
        }

        public int getBaselineSurveyResourceId()
        {
            return baselineSurveyResourceId;
        }

        public int getEvaluationSurveyResourceId()
        {
            return evaluationSurveyResourceId;
        }

        public String getAnswer(int questionId)
        {
            try
            {
                SurveyManager sm = Learnweb.getInstance().getSurveyManager();

                // depending on the columnId we have to select the correct survey
                if(questionId == 0)
                    return "|";
                else if(questionId >= 3 && questionId <= 44) // baseline survey
                {
                    if(getBaselineSurveyResourceId() < 0)
                        return "";

                    return sm.getSurveyResource(getBaselineSurveyResourceId()).getAnswersOfUser(getUserId()).getAnswer(questionId);
                }
                else if(questionId >= 971 && questionId <= 1005)
                {
                    if(getEvaluationSurveyResourceId() < 0)
                        return "";

                    return sm.getSurveyResource(getEvaluationSurveyResourceId()).getAnswersOfUser(getUserId()).getAnswer(questionId);
                }
                else if(questionId >= 71 && questionId <= 397 || questionId >= 1006 && questionId <= 1010) // asessment survey
                {
                    return peerAssessmentPair.getAssessment().getSubmittedAnswersOfAllUsers().get(0).getAnswer(questionId); // only one user must have submitted the assessment survey
                }
                else // asessment survey
                {
                    return peerAssessmentPair.getPeerAssessmentUserAnswers().getAnswer(questionId);
                }

            }
            catch(Exception e)
            {
                log.error("Can't load survey answer");
            }
            return "[System error]";
        }

    }
}
