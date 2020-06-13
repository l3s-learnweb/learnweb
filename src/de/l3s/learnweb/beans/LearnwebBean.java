package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

@Named
@ApplicationScoped
public class LearnwebBean implements Serializable {
    private static final long serialVersionUID = 1286475643761742147L;
    private static final Logger log = LogManager.getLogger(LearnwebBean.class);

    private final transient Learnweb learnweb;
    private boolean maintenanceMode = false;
    private boolean developmentMode = false;

    public LearnwebBean() throws ClassNotFoundException, SQLException {
        String serverUrl = Faces.getRequestBaseURL();
        learnweb = Learnweb.createInstance(serverUrl);
    }

    @PostConstruct
    public void init() {
        // initialize stuff which is not required by console tasks
        learnweb.initLearnwebServer();
        developmentMode = Faces.isDevelopment();
    }

    public Learnweb getLearnweb() {
        if (null == learnweb) {
            log.error("LearnwebBean: learnweb is null -> redirect");
            throw new IllegalStateException("Unable to initialize Learnweb.");
        }
        return learnweb;
    }

    @PreDestroy
    public void onDestroy() {
        learnweb.onDestroy();
    }

    /**
     * Returns the path to the users profile image or a default image if no available.
     */
    public String getProfileImage(User user) throws SQLException {
        if (user != null) {
            String url = user.getImage();

            if (null != url) {
                return url;
            }
        }
        return learnweb.getServerUrl() + "/resources/images/no-profile-picture.jpg";
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public boolean isDevelopmentMode() {
        return developmentMode;
    }

    public String getTrackerApiKey() {
        return this.learnweb.getProperties().getProperty("TRACKER_API_KEY");
    }

}
