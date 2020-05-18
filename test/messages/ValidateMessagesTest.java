package messages;

import static org.junit.jupiter.api.Assertions.*;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

public class ValidateMessagesTest {
    private static final List<String> LOCALES = Arrays.asList("de", "de_DE_Archive", "en", "en_UK_Archive", "it", "pt", "es", "uk");
    private static final String MESSAGES_PATH = "de/l3s/learnweb/lang/messages";

    @Test
    void validateAllMessages() {
        for (String locale : LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES_PATH, Locale.forLanguageTag(locale));

            Map<String, String> messages = getMessagesFromBundle(bundle);
            validateMessages(locale, messages);
        }
    }

    private static Map<String, String> getMessagesFromBundle(final ResourceBundle bundle) {
        final Map<String, String> messages = new HashMap<>();
        Collections.list(bundle.getKeys()).forEach(key -> messages.put(key, bundle.getString(key)));
        return messages;
    }

    private static void validateMessages(final String locale, final Map<String, String> messages) {
        for (Map.Entry<String, String> message : messages.entrySet()) {
            try {
                new MessageFormat(message.getValue());
            } catch (Exception e) {
                fail("The key '" + message.getKey() + "' of '" + locale + "' is not valid!", e);
            }
        }
    }
}
