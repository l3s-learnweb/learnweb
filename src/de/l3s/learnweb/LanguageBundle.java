package de.l3s.learnweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.UtilBean;

/**
 * Enables constant substitution in property values
 *
 * Example:
 * prop1=Lorem
 * prop2=#[prop1] ipsum
 *
 * Inspired by: http://www2.sys-con.com/ITSG/virtualcd/Java/archives/0612/mair/index.html
 *
 * @author Kemkes
 *
 */
public class LanguageBundle extends ResourceBundle
{
    protected static final Logger log = Logger.getLogger(LanguageBundle.class);
    protected static final String BASE_NAME = "de.l3s.learnweb.lang.messages";
    protected static final ConcurrentHashMap<Locale, ResourceBundle> cache = new ConcurrentHashMap<>(7);
    static
    {
        cache.put(new Locale("xx"), new DebugBundle());
    }

    private Map<String, String> values;

    public LanguageBundle()
    {
        this(BASE_NAME, FacesContext.getCurrentInstance().getViewRoot().getLocale());
    }

    public LanguageBundle(Locale locale)
    {
        this(BASE_NAME, locale);
    }

    public LanguageBundle(String baseName, Locale locale)
    {
        setParent(getLanguageBundle(baseName, locale));
    }

    public static ResourceBundle getLanguageBundle(String baseName, Locale locale)
    {
        return cache.computeIfAbsent(locale, loc -> new LanguageBundle(ResourceBundle.getBundle(baseName, loc)));
    }

    public LanguageBundle(ResourceBundle sourceBundle)
    {
        ArrayList<String> keys = Collections.list(sourceBundle.getKeys());

        values = new HashMap<>(keys.size());
        for(String key : keys)
        {
            String value = sourceBundle.getString(key);

            values.put(key, value);
        }

        boolean replacedAtLeastOneConstant;
        do
        {
            replacedAtLeastOneConstant = false;

            for(Entry<String, String> entry : values.entrySet())
            {
                String newValue = substituteValue(entry.getValue());
                if(!entry.getValue().equals(newValue))
                {
                    replacedAtLeastOneConstant = true;
                    entry.setValue(newValue);
                }
            }
        }
        while(replacedAtLeastOneConstant);
    }

    private static final String START_CONST = "#[";
    private static final String END_CONST = "]";

    private String substituteValue(String value)
    {
        if(value == null)
            return null;

        int beginIndex = 0;
        int startName; // index of the  constant, if any

        StringBuilder sb = new StringBuilder(value); // build the new value

        while(true)
        {
            startName = sb.indexOf(START_CONST, beginIndex);
            if(startName == -1)
                break;

            int endName = sb.indexOf(END_CONST, startName);
            if(endName == -1)
            {
                // Terminating symbol not found
                // Return the value as is

                return value;
            }

            String constName = sb.substring(startName + START_CONST.length(), endName);
            String constValue = values.get(constName);

            if(constValue == null || constValue.contains(START_CONST))
            {
                // Property name not found or contains variable
                // Ignore this variable
                beginIndex = endName + END_CONST.length();
                continue;
            }

            // Insert the constant value into the
            // original property value
            sb.replace(startName, endName + END_CONST.length(), constValue);

            // continue checking for constants at this index
            beginIndex += constValue.length();
        }

        return sb.toString();
    }

    @Override
    protected Object handleGetObject(String key)
    {
        return values != null ? values.get(key) : parent.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys()
    {
        return values != null ? Collections.enumeration(values.keySet()) : parent.getKeys();
    }

    private static List<Locale> supportedLocales = Collections.synchronizedList(new ArrayList<Locale>());

    /**
     *
     * @return Supported frontend locales as defined in faces-config.xml
     */
    public static List<Locale> getSupportedLocales()
    {
        if(supportedLocales.isEmpty())
        {
            Iterator<Locale> iterator = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();

            iterator.forEachRemaining(supportedLocales::add);
        }
        return supportedLocales;
    }

    public static void main(String[] args)
    {

        Locale locale = new Locale("de", "DE", "");

        log.debug(locale + "; " + locale.toLanguageTag());

        ResourceBundle bundle = new LanguageBundle(locale);
        log.debug(bundle.getString("homepageTitle"));
        //bundle = new LanguageBundle(locale);
        log.debug(bundle.getString("register_account_already_wizard"));
        log.debug(bundle.getString("register_lw_account_wizard"));

        locale = new Locale("de");
        bundle = new LanguageBundle(locale);
        log.debug(bundle.getString("homepageTitle"));
        bundle = new LanguageBundle(locale);
        log.debug(bundle.getString("username"));

        log.debug(UtilBean.getLocaleMessage(locale, "Glossary.description"));

    }

    /**
     *
     * This bundle returns always the key as it's value. This helps to find the appropriate key in the frontend
     *
     */
    private static class DebugBundle extends ResourceBundle
    {
        @Override
        protected Object handleGetObject(String key)
        {
            return "#" + key + "#";
        }

        @Override
        public Enumeration<String> getKeys()
        {
            return null;
        }
    }

}
