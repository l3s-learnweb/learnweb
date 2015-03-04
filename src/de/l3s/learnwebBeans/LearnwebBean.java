package de.l3s.learnwebBeans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
@ApplicationScoped
public class LearnwebBean implements Serializable
{
    private static final long serialVersionUID = 1286475643761742147L;

    private transient Learnweb learnweb;
    private LinkedList<LocaleContainer> supportedLocales = new LinkedList<LocaleContainer>();
    private String contextUrl;

    public LearnwebBean() throws IOException
    {
	// load supported languages. See WEB-INF/faces-config.xml 
	Iterator<Locale> locales = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
	while(locales.hasNext())
	{
	    supportedLocales.add(new LocaleContainer(locales.next()));
	}

	ExternalContext ext = FacesContext.getCurrentInstance().getExternalContext();

	if(ext.getRequestServerPort() == 80 || ext.getRequestServerPort() == 443)
	    contextUrl = ext.getRequestScheme() + "://" + ext.getRequestServerName() + ext.getRequestContextPath();
	else
	    contextUrl = ext.getRequestScheme() + "://" + ext.getRequestServerName() + ":" + ext.getRequestServerPort() + ext.getRequestContextPath();

	learnweb = Learnweb.getInstance();
	learnweb.setContextUrl(contextUrl);
    }

    private static void addMessage(String message)
    {
	FacesContext.getCurrentInstance().addMessage("message", new FacesMessage(FacesMessage.SEVERITY_FATAL, message, null));
    }

    /**
     * 
     * @return Returns the servername + contextpath. For the default installation this is: http://learnweb.l3s.uni-hannover.de
     */
    public String getContextUrl()
    {
	return contextUrl; // because we don't use httpS we can cache the url, change it if you want to use httpS too
    }

    /**
     * 
     * @return example for a local installation: http://localhost:8080/jlw/lw/
     */
    public String getBaseUrl()
    {
	ExternalContext ext = FacesContext.getCurrentInstance().getExternalContext();

	String path = ext.getRequestServletPath();
	path = path.substring(0, path.indexOf("/", 1) + 1);

	return contextUrl + path;
    }

    public Learnweb getLearnweb()
    {
	if(null == learnweb)
	{
	    UtilBean.redirect(getBaseUrl() + "error.jsf");
	}
	return learnweb;
    }

    public void addGlobalErrorMessage()
    {
	if(null == learnweb)
	{
	    addMessage("Could not start learnweb");
	}

    }

    @PreDestroy
    public void onDestroy()
    {
	learnweb.onDestroy();
    }

    public LinkedList<LocaleContainer> getSupportedLocales()
    {
	return supportedLocales;
    }

    public class LocaleContainer
    {
	private Locale locale;
	private String countryCode;
	private String languageName;

	public LocaleContainer(Locale locale)
	{
	    this.locale = locale;
	    this.countryCode = locale.getCountry().toLowerCase();
	    this.languageName = locale.getDisplayLanguage(locale);
	}

	public Locale getLocale()
	{
	    return locale;
	}

	public String getCountryCode()
	{
	    return countryCode;
	}

	public String getLanguageName()
	{
	    return languageName;
	}
    }

    /**
     * returns the path to the users profile image or a default image if no available
     * 
     * @param user
     * @return
     * @throws SQLException
     */
    public String getProfileImage(User user) throws SQLException
    {
	String url = user.getImage();

	if(null != url)
	    return url;

	return getContextUrl() + "/resources/image/no_profile.jpg";
    }

    /**
     * get a message from the message property files depending on the currently used local
     * 
     * @param msgKey
     * @param args
     * @return
     */
    public String getLocaleMessage(String msgKey)
    {
	return UtilBean.getLocaleMessage(msgKey);
    }
}
