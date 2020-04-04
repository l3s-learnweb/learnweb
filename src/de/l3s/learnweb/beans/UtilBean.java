package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.user.UserBean;

@Named
@ApplicationScoped
public class UtilBean implements Serializable
{
    private static final long serialVersionUID = 6252597111468136574L;
    private static final Logger log = Logger.getLogger(UtilBean.class);

    public static ExternalContext getExternalContext()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getExternalContext();
    }

    public static String getContextPath()
    {
        ServletContext servletContext = (ServletContext) getExternalContext().getContext();
        return servletContext.getContextPath();
    }

    /**
     * @return example http://learnweb.l3s.uni-hannover.de or http://localhost:8080/Learnweb-Tomcat
     */
    public static String getServerUrl()
    {
        try
        {
            ExternalContext ext = getExternalContext();

            if(ext.getRequestServerPort() == 80 || ext.getRequestServerPort() == 443)
                return ext.getRequestScheme() + "://" + ext.getRequestServerName() + ext.getRequestContextPath();
            else
                return ext.getRequestScheme() + "://" + ext.getRequestServerName() + ":" + ext.getRequestServerPort() + ext.getRequestContextPath();
        }
        catch(Exception e)
        {
            log.warn("Can't get server url. This is only expected in console mode");
            return "https://learnweb.l3s.uni-hannover.de";
        }
    }

    /**
     * This method is intended only for special use cases. Discuss with project leader before using it.
     *
     * @param redirectPath
     */
    public static void redirect(String redirectPath)
    {
        ExternalContext externalContext = getExternalContext();
        try
        {
            externalContext.redirect(getContextPath() + redirectPath);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Use ApplicationBean.getUserBean() instead. This method must only be called on a bean/facesContext not from any models.
     *
     * @return
     */
    @Deprecated
    public static UserBean getUserBean()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return (UserBean) fc.getApplication().getELResolver().getValue(fc.getELContext(), null, "userBean");
    }

    /**
     * This method should not be used any more. It will only work correctly when it is used in a faces context.
     * Move translations to the bean oder XHTML file. In beans you can use getLocaleMessage() in XHTML files use #{msg['a_pfrefix' += your_key]}
     *
     * The method is also used in many XHTML files. That's acceptable.
     * But o:outputformat could be used instead: http://showcase.omnifaces.org/components/outputFormat
     *
     */
    @Deprecated
    public static String getLocaleMessage(String msgKey, Object... args)
    {
        // guess locale
        Locale locale;
        try
        {
            locale = getUserBean().getLocale();
        }
        catch(Exception e)
        {
            log.error("Can't load current locale", e);
            locale = Locale.ENGLISH;
        }

        return LanguageBundle.getLocaleMessage(locale, msgKey, args);
    }
}
