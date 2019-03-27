package de.l3s.learnweb.resource.peerAssessment;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.Min;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class AdminPeerAssessmentPairBeen extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073496345L;
    //private static final Logger log = Logger.getLogger(AdminPeerAssessmentPairBeen.class);
    private static final String PREF_VIEW = "PeerAssessmentPair.view";

    @Min(value = 1)
    private int peerAssessmentId = 0;
    private List<PeerAssessmentPair> pairs;
    private boolean fullView;

    public AdminPeerAssessmentPairBeen()
    {
    }

    public void onLoad() throws SQLException
    {
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        pairs = getLearnweb().getPeerAssessmentManager().getPairsByPeerAssessmentId(peerAssessmentId);

        this.fullView = Boolean.parseBoolean(getPreference(PREF_VIEW, Boolean.toString(false)));
    }

    public List<PeerAssessmentPair> getPairs()
    {
        return pairs;
    }

    public int getPeerAssessmentId()
    {
        return peerAssessmentId;
    }

    public void setPeerAssessmentId(int peerAssessmentId)
    {
        this.peerAssessmentId = peerAssessmentId;
    }

    public boolean isFullView()
    {
        return fullView;
    }

    public void setFullView(boolean fullView)
    {
        setPreference(PREF_VIEW, Boolean.toString(fullView));
        this.fullView = fullView;
    }
}
