package de.l3s.learnweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

/**
 * Enables constant substitution in property values
 *
 * Example:
 * prop1=Lorem
 * prop2=${prop1} ipsum
 *
 * Inspired by: http://www2.sys-con.com/ITSG/virtualcd/Java/archives/0612/mair/index.html
 *
 * @author Kemkes
 *
 */
public class LanguageBundle extends ResourceBundle
{
    private final static Logger log = Logger.getLogger(LanguageBundle.class);

    protected static final String BASE_NAME = "de.l3s.learnweb.lang.messages";

    protected static final ConcurrentHashMap<Locale, LanguageBundle> cache = new ConcurrentHashMap<>(5);

    private Map<String, String> values;

    private Locale locale;

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
        LanguageBundle bundle = cache.computeIfAbsent(locale, loc -> new LanguageBundle(ResourceBundle.getBundle(baseName, loc)));

        setParent(bundle);

        //log.debug("requested: " + locale + "; got " + bundle.getLocale());
    }

    @Override
    public Locale getLocale()
    {
        return locale;
    }

    public LanguageBundle(ResourceBundle sourceBundle)
    {
        locale = sourceBundle.getLocale();
        log.debug("Init language bundle: " + locale);

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

    private final static String START_CONST = "${";
    private final static String END_CONST = "}";

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

            if(constValue == null || constValue.indexOf(START_CONST) != -1)
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
        //log.debug("handleGetObject " + ": " + key);

        return values != null ? values.get(key) : parent.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys()
    {
        return values != null ? Collections.enumeration(values.keySet()) : parent.getKeys();
    }

    public static void main(String[] args)
    {
        Locale locale = new Locale("de", "", "ama");

        log.debug(locale + "; " + locale.toLanguageTag());

        ResourceBundle bundle = new LanguageBundle(locale);
        log.debug(bundle.getString("homepageTitle"));
        //bundle = new LanguageBundle(locale);
        log.debug(bundle.getString("username"));

        locale = new Locale("de");
        bundle = new LanguageBundle(locale);
        System.out.println(bundle.getString("homepageTitle"));
        bundle = new LanguageBundle(locale);
        System.out.println(bundle.getString("username"));
        /*
        locale = new Locale("de", "", "AMA");
        System.out.println(locale.hashCode());
        locale = new Locale("de", "", "ama");
        System.out.println(locale.hashCode());
        locale = new Locale("de", "", "AMA");
        System.out.println(locale.hashCode());
        locale = new Locale("de");
        System.out.println(locale.hashCode());
        locale = new Locale("DE", "", "");
        System.out.println(locale.hashCode());
        */
    }

}
