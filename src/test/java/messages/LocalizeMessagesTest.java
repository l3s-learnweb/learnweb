package messages;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.l3s.util.MessagesHelper;

public class LocalizeMessagesTest {

    @Test
    @Disabled("Right now many languages missing translations")
    void testLocalesToHaveAllBaseKeys() throws IOException {
        List<String> locales = MessagesHelper.getLocales(Collections.singleton("en"));
        Properties baseMessages = MessagesHelper.getMessagesForLocale(null);

        for (String locale : locales) {
            HashSet<String> missing = new HashSet<>();
            Properties localeMessages = MessagesHelper.getMessagesForLocale(locale);

            baseMessages.forEach((key, value) -> {
                if (!localeMessages.containsKey(key)) {
                    missing.add(key.toString());
                }
            });

            if (!missing.isEmpty()) {
                fail("Locale '" + locale + "' missing " + missing.size() + " keys: " + missing);
            }
        }
    }

    @Test
    void testLocalesToHaveOnlyKeysDeclaredInBaseLocale() throws IOException {
        List<String> locales = MessagesHelper.getLocales(Collections.singleton("en"));
        Properties baseMessages = MessagesHelper.getMessagesForLocale(null);

        for (String locale : locales) {
            HashSet<String> obsolete = new HashSet<>();
            Properties localeMessages = MessagesHelper.getMessagesForLocale(locale);

            localeMessages.keySet().forEach(key -> {
                if (!baseMessages.containsKey(key)) {
                    obsolete.add(key.toString());
                }
            });

            if (!obsolete.isEmpty()) {
                fail("Locale '" + locale + "' contains " + obsolete.size() + " keys that no more exist (in default messages file): " + obsolete);
            }
        }
    }
}
