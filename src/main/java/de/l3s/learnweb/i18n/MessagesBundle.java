package de.l3s.learnweb.i18n;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.omnifaces.util.Faces;

public class MessagesBundle extends ResourceBundle {
    private static final ResourceBundle.Control control = new DynamicControl();
    private static final ConcurrentHashMap<Locale, ResourceBundle> cache = new ConcurrentHashMap<>();

    public static ResourceBundle of(Locale locale) {
        return cache.computeIfAbsent(locale, local -> ResourceBundle.getBundle("lang", local, control));
    }

    private final Locale locale;

    public MessagesBundle() { // this is required by JSF because we can't pass arguments to the default constructor
        this(Faces.getLocale());
    }

    public MessagesBundle(Locale locale) {
        this.locale = locale;
        setParent(of(locale));
    }

    @Override
    protected Object handleGetObject(String key) {
        return parent.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return parent.getKeys();
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public String format(String msgKey, Object... args) {
        return format(this, msgKey, args);
    }

    public static String format(Locale locale, String msgKey, Object... args) {
        return format(of(locale), msgKey, args);
    }

    public static String format(ResourceBundle bundle, String msgKey, Object... args) {
        String msg;
        try {
            msg = bundle.getString(msgKey);
            if (args != null && args.length > 0) {
                MessageFormat format = new MessageFormat(msg);
                msg = format.format(args);
            }
        } catch (MissingResourceException | IllegalArgumentException e) {
            msg = msgKey;
        }
        return msg;
    }
}
