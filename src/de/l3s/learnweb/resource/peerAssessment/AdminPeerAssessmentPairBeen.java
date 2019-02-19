package de.l3s.learnweb.resource.peerAssessment;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class AdminPeerAssessmentPairBeen extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073496345L;
    //private static final Logger log = Logger.getLogger(AdminPeerAssessmentPairBeen.class);

    private int peerAssessmentId = 0;
    private List<PeerAssessmentPair> pairs;
    private boolean fullView = false;

    public AdminPeerAssessmentPairBeen()
    {
    }

    public void onLoad()
    {
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        try
        {
            pairs = getLearnweb().getPeerAssessmentManager().getPairsByPeerAssessmentId(peerAssessmentId);
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
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
        this.fullView = fullView;
    }
}
