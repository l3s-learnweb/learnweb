package de.l3s.learnweb.gdpr.beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.submission.Submission;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

/**
 * YourSubmissionsBean is responsible for displaying user submissions.
 */
@Named
@ViewScoped
public class YourSubmissionsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -7818173987582995716L;
    private static final Logger log = Logger.getLogger(YourSubmissionsBean.class);

    private List<Submission> userSubmissions;

    public YourSubmissionsBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        this.userSubmissions = this.getLearnweb().getSubmissionManager().getSubmissionsByUser(user);
    }

    public List<Submission> getUserSubmissions()
    {
        return userSubmissions;
    }
}
