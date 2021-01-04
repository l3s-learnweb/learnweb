package messages;

import static messages.MessagesTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

public class ValidateMessagesTest {

    @Test
    void validateAllMessages() throws IOException {
        for (String locale : getLocales()) {
            ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES_BUNDLE, Locale.forLanguageTag(locale));

            Map<String, String> messages = getMessagesFromBundle(bundle);
            validateMessages(locale, messages);
            assertTrue(true);
        }
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
