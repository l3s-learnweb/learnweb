package messages;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class LocalizeMessagesTest
{
    private static final String MESSAGES_PATH = "de/l3s/learnweb/lang/messages";

    @Test
    void testLocalesToHaveAllBaseKeys() throws IOException
    {
        Properties baseMessages = getMessagesForLocale(null);

        assertAll(Stream.of("de", "es", "it", "pt", "uk").map(locale -> () ->
        {
            HashSet<String> missingMessages = new HashSet<>();
            Properties localeMessages = getMessagesForLocale(locale);

            baseMessages.keySet().forEach(key ->
            {
                if (!localeMessages.containsKey(key))
                {
                    missingMessages.add(key.toString());
                }
            });

            if (!missingMessages.isEmpty())
                fail("Locale '" + locale + "' missing " + missingMessages.size() + " keys: " + missingMessages.toString());
        }));
    }

    @Test
    void testLocalesToHaveOnlyKeysDeclaredInBaseLocale() throws IOException
    {
        Properties baseMessages = getMessagesForLocale(null);

        assertAll(Stream.of("de", "de_DE_AMA", "de_DE_Archive", "en", "en_UK_Archive", "it", "pt", "es", "uk", "xy").map(locale -> () ->
        {
            HashSet<String> extraMessages = new HashSet<>();
            Properties localeMessages = getMessagesForLocale(locale);

            localeMessages.keySet().forEach(key ->
            {
                if (!baseMessages.containsKey(key))
                {
                    extraMessages.add(key.toString());
                }
            });

            if (!extraMessages.isEmpty())
                fail("Locale '" + locale + "' contains " + extraMessages.size() + " keys that not exist in default: " + extraMessages.toString());
        }));
    }

    @Test
    void testEnLocaleToNotContainsKeys() throws IOException
    {
        Properties baseMessages = getMessagesForLocale("en");

        assertTrue(baseMessages.isEmpty(), "The 'en' locale should not contains any keys. Add all English translations to default.");
    }

    private static Properties getMessagesForLocale(String locale) throws IOException
    {
        if (locale == null) locale = "";
        if (!locale.isEmpty()) locale = "_" + locale;

        Properties properties = new Properties();
        InputStream is = LocalizeMessagesTest.class.getClassLoader().getResourceAsStream(MESSAGES_PATH + locale + ".properties");
        assert is != null;
        properties.load(is);
        return properties;
    }
}
