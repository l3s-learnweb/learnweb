package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Organisation.Option;

/**
 * The privacy statement should show the tracker information only when the feature is enabled for
 * the users organisation. But the information needs to be shown already during the registration process
 *
 * @author Philipp Kemkes
 */
@Named
@RequestScoped
public class PrivacyStatementBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -6243573356677232210L;

    private boolean trackingEnabled = false;
    private int organisationId;

    public void onLoad() {
        Organisation organisation = null;

        if (organisationId != 0) {
            organisation = dao().getOrganisationDao().findByIdOrElseThrow(organisationId);
        } else if (getUser() != null) {
            organisation = getUser().getOrganisation();
        }

        if (organisation != null) {
            trackingEnabled = organisation.getOption(Option.Privacy_Tracker_disabled);
        }
    }

    /**
     * @return True either if tracking is enabled for the given organisation id or if no id was gives the setting for
     * the currently logged in user is returned. If the current user isn't logged in it returns false
     */
    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }
}
