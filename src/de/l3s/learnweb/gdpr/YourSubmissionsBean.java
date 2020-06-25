package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.resource.submission.Submission;
import de.l3s.learnweb.user.User;

/**
 * YourSubmissionsBean is responsible for displaying user submissions.
 */
@Named
@ViewScoped
public class YourSubmissionsBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -7818173987582995716L;
    //private static final Logger log = LogManager.getLogger(YourSubmissionsBean.class);

    private List<Submission> userSubmissions;

    public YourSubmissionsBean() throws SQLException {
        User user = getUser();
        BeanAsserts.authorized(user);

        this.userSubmissions = this.getLearnweb().getSubmissionManager().getSubmissionsByUser(user);
    }

    public List<Submission> getUserSubmissions() {
        return userSubmissions;
    }
}
