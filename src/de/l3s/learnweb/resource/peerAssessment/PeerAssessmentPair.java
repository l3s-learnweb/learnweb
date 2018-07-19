package de.l3s.learnweb.resource.peerAssessment;

import java.io.Serializable;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.resource.survey.SurveyUserAnswers;
import de.l3s.learnweb.user.User;

public class PeerAssessmentPair implements Serializable
{
    private static final long serialVersionUID = -4241273453267455330L;
    private final int id;
    private final int assessorUserId;
    private final int assessedUserId;
    private final int peerAssessmentSurveyResourceId;
    private int assessmentSurveyResourceId;
    private final int submissionId;
    private transient User assessorUser;
    private transient User assessedUser;
    private transient SurveyUserAnswers peerAssessmentUserAnswers;

    /**
     *
     * @param id
     * @param assessorUserId
     * @param assessedUserId
     * @param peerAssessmentSurveyResourceId
     * @param assessmentSurveyResourceId
     * @param submissionId
     */
    public PeerAssessmentPair(int id, int assessorUserId, int assessedUserId, int peerAssessmentSurveyResourceId, int assessmentSurveyResourceId, int submissionId)
    {
        super();
        this.id = id;
        this.assessorUserId = assessorUserId;
        this.assessedUserId = assessedUserId;
        this.peerAssessmentSurveyResourceId = peerAssessmentSurveyResourceId;
        this.assessmentSurveyResourceId = assessmentSurveyResourceId;
        this.submissionId = submissionId;
    }

    public int getPeerAssessmentSurveyResourceId()
    {
        return peerAssessmentSurveyResourceId;
    }

    public int getAssessmentSurveyResourceId()
    {
        return assessmentSurveyResourceId;
    }

    public SurveyUserAnswers getPeerAssessmentUserAnswers() throws SQLException
    {
        if(null == peerAssessmentUserAnswers) // load survey details
            peerAssessmentUserAnswers = getPeerAssessment().getAnswersOfUser(assessorUserId);
        return peerAssessmentUserAnswers;
    }

    public User getAssessorUser() throws SQLException
    {
        if(assessorUser == null)
            assessorUser = Learnweb.getInstance().getUserManager().getUser(assessorUserId);
        return assessorUser;
    }

    public User getAssessedUser() throws SQLException
    {
        if(assessedUser == null)
            assessedUser = Learnweb.getInstance().getUserManager().getUser(assessedUserId);
        return assessedUser;
    }

    public SurveyResource getPeerAssessment() throws SQLException
    {
        return (SurveyResource) Learnweb.getInstance().getResourceManager().getResource(peerAssessmentSurveyResourceId);
    }

    public SurveyResource getAssessment() throws SQLException
    {
        return (SurveyResource) Learnweb.getInstance().getResourceManager().getResource(assessmentSurveyResourceId);
    }

    public int getId()
    {
        return id;
    }

    public int getAssessorUserId()
    {
        return assessorUserId;
    }

    public int getAssessedUserId()
    {
        return assessedUserId;
    }

    public int getSubmissionId()
    {
        return submissionId;
    }

    @Override
    public String toString()
    {
        return "PeerAssessmentPair [id=" + id + ", assessorUserId=" + assessorUserId + ", assessedUserId=" + assessedUserId + ", surveyResourceId=" + peerAssessmentSurveyResourceId + ", submissionId=" + submissionId + "]";
    }

    public void setAssessmentSurveyResourceId(int assessmentSurveyResourceId)
    {
        this.assessmentSurveyResourceId = assessmentSurveyResourceId;
    }

}
