package messages;

import static messages.MessagesTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class LocalizeMessagesTest {

    @Test
    void testLocalesToHaveAllBaseKeys() throws IOException {
        Properties baseMessages = getMessagesForLocale(null);

        assertAll(getLocales(Collections.singletonList("en")).stream().map(locale -> () -> {
            HashSet<String> missingMessages = new HashSet<>();
            Properties localeMessages = getMessagesForLocale(locale);

            baseMessages.forEach((key, value) -> {
                if (!localeMessages.containsKey(key)) {
                    missingMessages.add(key.toString());
                }
            });

            if (!missingMessages.isEmpty()) {
                fail("Locale '" + locale + "' missing " + missingMessages.size() + " keys: " + missingMessages);
            }
        }));
    }

    @Test
    void testLocalesToHaveOnlyKeysDeclaredInBaseLocale() throws IOException {
        Properties baseMessages = getMessagesForLocale(null);

        assertAll(getLocales(Collections.singleton("en")).stream().map(locale -> () -> {
            HashSet<String> extraMessages = new HashSet<>();
            Properties localeMessages = getMessagesForLocale(locale);

            localeMessages.keySet().forEach(key -> {
                if (!baseMessages.containsKey(key)) {
                    extraMessages.add(key.toString());
                }
            });

            if (!extraMessages.isEmpty()) {
                fail("Locale '" + locale + "' contains " + extraMessages.size() + " keys that not exist in default: " + extraMessages);
            }
        }));
    }
}
