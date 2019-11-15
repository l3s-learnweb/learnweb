package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.user.UserBean;

@Named
@ApplicationScoped
public class UtilBean implements Serializable
{
    private final static long serialVersionUID = 6252597111468136574L;
    private final static Logger log = Logger.getLogger(UtilBean.class);

    public static ExternalContext getExternalContext()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getExternalContext();
    }

    public static Object getManagedBean(String beanName)
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getApplication().getELResolver().getValue(fc.getELContext(), null, beanName);
    }

    public static UserBean getUserBean()
    {
        return (UserBean) getManagedBean("userBean");
    }

    public static void redirect(String redirectPath)
    {
        ExternalContext externalContext = getExternalContext();

        try
        {
            externalContext.redirect(redirectPath);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String getLocaleMessage(String msgKey, Object... args)
    {
        // guess locale
        Locale locale;
        try
        {
            locale = UtilBean.getUserBean().getLocale();
        }
        catch(Exception e)
        {
            log.error("Can't load current locale", e);
            locale = Locale.ENGLISH;
        }

        return getLocaleMessage(locale, msgKey, args);
    }

    public static String getLocaleMessage(Locale locale, String msgKey, Object... args)
    {
        ResourceBundle bundle = LanguageBundle.getLanguageBundle("de.l3s.learnweb.lang.messages", locale);

        String msg = "";
        try
        {
            msg = bundle.getString(msgKey);
            if(args != null)
            {
                MessageFormat format = new MessageFormat(msg);
                msg = format.format(args);
            }
        }
        catch(MissingResourceException e)
        {
            //	    log.warn("Missing translation for key: " + msgKey);
            msg = msgKey;
        }
        catch(IllegalArgumentException e)
        {
            log.error("Can't translate msgKey=" + msgKey + " with msg=" + msg + "; May happen if the msg contains unexpected curly brackets.", e);

            msg = msgKey;
        }
        return msg;
    }

    public static String getLocaleMessage(String msgKey)
    {
        Object[] args = new Object[0];
        return getLocaleMessage(msgKey, args);
    }
}
