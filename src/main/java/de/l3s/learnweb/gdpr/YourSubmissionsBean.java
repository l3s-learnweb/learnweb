package de.l3s.learnweb.gdpr;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.submission.Submission;
import de.l3s.learnweb.user.User;

/**
 * YourSubmissionsBean is responsible for displaying user submissions.
 */
@Named
@ViewScoped
public class YourSubmissionsBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -7818173987582995716L;
    //private static final Logger log = LogManager.getLogger(YourSubmissionsBean.class);

    private List<Submission> userSubmissions;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        this.userSubmissions = dao().getSubmissionDao().findByUser(user);
    }

    public List<Submission> getUserSubmissions() {
        return userSubmissions;
    }
}