package messages;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import de.l3s.util.MessagesHelper;

public class ValidateMessagesTest {

    /**
     * You can use this test to check messages syntax, if needed.
     */
    @Test
    void validateMessage() {
        String result = MessageFormat.format("On {1, date}, there was {2} on planet {0, number, integer}.",
            7, new Date(1564660800000L), "a disturbance in the Force");
        assertEquals("On 1 Aug 2019, there was a disturbance in the Force on planet 7.", result);
    }

    @Test
    void validateAllMessages() throws IOException {
        for (String locale : MessagesHelper.getLocales()) {
            ResourceBundle bundle = ResourceBundle.getBundle(MessagesHelper.MESSAGES_BUNDLE, Locale.forLanguageTag(locale));

            Map<String, String> messages = MessagesHelper.getMessagesFromBundle(bundle);
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
