package de.l3s.learnweb.resource.peerAssessment;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Named;
import javax.enterprise.context.RequestScoped;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.survey.SurveyResource;

@Named
@RequestScoped
public class AssessmentResultBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6159753194487272716L;
    private List<PeerAssessmentPair> peerAssessmentPairs;
    private SurveyResource mandatorySurvey; //  this survey must be answered before results can be viewed
    private boolean mandatorySurveySubmitted = true;

    public AssessmentResultBean() throws SQLException
    {
        if(getUser() == null)
            return;

        peerAssessmentPairs = getUser().getAssessedPeerAssessments();

        // hardcoded until other courses need it too
        mandatorySurvey = (SurveyResource) getLearnweb().getResourceManager().getResource(216012); // eu made4 all Evaluation Form
        mandatorySurveySubmitted = mandatorySurvey.isSubmitted(getUser().getId());
    }

    public List<PeerAssessmentPair> getPeerAssessmentPairs()
    {
        return peerAssessmentPairs;
    }

    public SurveyResource getMandatorySurvey()
    {
        return mandatorySurvey;
    }

    public boolean isMandatorySurveySubmitted()
    {
        return mandatorySurveySubmitted;
    }

}
