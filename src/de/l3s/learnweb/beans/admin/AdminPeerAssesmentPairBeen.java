package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.PeerAssessmentManager.PeerAssesmentPair;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;

@ManagedBean
@ViewScoped
public class AdminPeerAssesmentPairBeen extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073496345L;
    private static final Logger log = Logger.getLogger(AdminPeerAssesmentPairBeen.class);

    private int peerAssementId = 0;
    private List<PeerAssesmentPair> pairs;

    public AdminPeerAssesmentPairBeen()
    {
    }

    public void onLoad()
    {
        log.debug("onLoad");
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        try
        {
            pairs = getLearnweb().getPeerAssessmentManager().getPairsByPeerAssessmentId(peerAssementId);
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public List<PeerAssesmentPair> getPairs()
    {
        return pairs;
    }

    public int getPeerAssementId()
    {
        return peerAssementId;
    }

    public void setPeerAssementId(int peerAssementId)
    {
        this.peerAssementId = peerAssementId;
    }

}
