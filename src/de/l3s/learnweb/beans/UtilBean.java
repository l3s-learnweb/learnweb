package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.group.GroupDetailBean;
import de.l3s.learnweb.resource.AddResourceBean;
import de.l3s.learnweb.resource.MyResourcesBean;
import de.l3s.learnweb.user.UserBean;

@ApplicationScoped
@ManagedBean
public class UtilBean implements Serializable
{
    private final static long serialVersionUID = 6252597111468136574L;
    private final static Logger log = Logger.getLogger(UtilBean.class);

    @Deprecated
    public boolean isSearchPage()
    {
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        if(viewId.contains("search.xhtml"))
            return true;
        else
            return false;
    }

    @Deprecated
    public boolean isViewResultSetPage()
    {
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        if(viewId.contains("view_resultset.xhtml"))
            return true;
        else
            return false;
    }

    // ------------------------

    public static ExternalContext getExternalContext()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getExternalContext();
    }

    public static LearnwebBean getLearnwebBean()
    {
        return (LearnwebBean) getManagedBean("learnwebBean");
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

    public static GroupDetailBean getGroupDetailBean()
    {
        return (GroupDetailBean) getManagedBean("groupDetailBean");
    }

    public static MyResourcesBean getMyResourcesBean()
    {
        return (MyResourcesBean) getManagedBean("myResourcesBean");
    }

    public static AddResourceBean getAddResourceBean()
    {
        return (AddResourceBean) getManagedBean("addResourceBean");
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
        // get bundle by locale. The bundles are cached in the ResourceBundle class.
        ResourceBundle bundle = ResourceBundle.getBundle("de.l3s.learnweb.lang.messages", locale);

        String msg;
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
            //	    log.error("Missing translation for key: " + msgKey);
            msg = msgKey;
        }
        return msg;
    }

    public static String getLocaleMessage(String msgKey)
    {
        Object[] args = new Object[0];
        return getLocaleMessage(msgKey, args);
    }

    // ------------------------

    public static int time()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public Date StringToDate(String dateStr) throws ParseException
    {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        return format.parse(dateStr);
    }

    public String addSlashes(String input)
    {
        return input.replace("\"", "\\\"");
    }
}
