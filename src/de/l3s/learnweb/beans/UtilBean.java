package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.user.UserBean;
import de.l3s.util.bean.BeanHelper;

@Named
@ApplicationScoped
public class UtilBean implements Serializable {
    private static final long serialVersionUID = 6252597111468136574L;
    private static final Logger log = LogManager.getLogger(UtilBean.class);

    /**
     * This method shall only be used in XHTML files!
     *
     * @return The translation for the msgKey. Escaped so that it can be included in JS strings.
     */
    public String getLocaleMessageEscaped(String msgKey, Object... args) {
        return escapeJS(getLocaleMessage(msgKey, args));
    }

    public String escapeJS(String input) {
        return StringEscapeUtils.escapeEcmaScript(input);
    }

    /**
     * This method is intended only for special use cases. Discuss with project leader before using it.
     */
    public static void redirect(String redirectPath) {
        try {
            ExternalContext externalContext = BeanHelper.getExternalContext();
            ServletContext servletContext = (ServletContext) externalContext.getContext();
            externalContext.redirect(servletContext.getContextPath() + redirectPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Use ApplicationBean.getUserBean() instead. This method must only be called on a bean/facesContext not from any models.
     */
    @Deprecated
    public static UserBean getUserBean() {
        FacesContext fc = FacesContext.getCurrentInstance();
        return (UserBean) fc.getApplication().getELResolver().getValue(fc.getELContext(), null, "userBean");
    }

    /**
     * This method should not be used any more. It will only work correctly when it is used in a faces context.
     * Move translations to the bean or XHTML file. In beans you can use getLocaleMessage() in XHTML files use #{msg['a_prefix' += your_key]}
     *
     * The method is also used in many XHTML files. Consider using o:outputFormat or of:formatX() instead:
     * - http://showcase.omnifaces.org/components/outputFormat
     * - http://showcase.omnifaces.org/functions/Strings
     */
    @Deprecated
    public static String getLocaleMessage(String msgKey, Object... args) {
        // guess locale
        Locale locale;
        try {
            locale = getUserBean().getLocale();
        } catch (Exception e) {
            log.error("Can't load current locale", e);
            locale = Locale.ENGLISH;
        }

        return LanguageBundle.getLocaleMessage(locale, msgKey, args);
    }
}
