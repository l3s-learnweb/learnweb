package de.l3s.learnweb.beans;

import de.l3s.learnweb.Organisation;
import de.l3s.learnweb.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.io.Serializable;
import java.sql.SQLException;

@ManagedBean
@RequestScoped
public class ConfirmRequiredBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(ConfirmRequiredBean.class);
    private static final long serialVersionUID = 934105342636869805L;

    public ConfirmRequiredBean()
    {
    }

    public void onLoad() throws SQLException
    {
        if (isConfirmed()) {
            Organisation userOrganisation = getUser().getOrganisation();
            //UtilBean.redirect("/lw/" + userOrganisation.getWelcomePage() + "?faces-redirect=true");
        }
    }

    public boolean isConfirmed()
    {
        return getUser() != null && getUser().getIsEmailConfirmed();
    }
}
