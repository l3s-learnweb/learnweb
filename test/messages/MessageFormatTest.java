package messages;

import static org.junit.jupiter.api.Assertions.*;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class MessageFormatTest {
    @Test
    void formatDate() {
        MessageFormat messageFormat = new MessageFormat("{0,date,medium} {0,time,short}", Locale.ENGLISH);
        assertEquals("Aug 1, 2019 2:00 PM", messageFormat.format(new Object[] {new Date(1564660800000L)}));
    }

    @Test
    void formatDatePattern() {
        MessageFormat messageFormat = new MessageFormat("{0,date,MMM d, yyyy h:mm a}", Locale.ENGLISH);
        assertEquals("Aug 1, 2019 2:00 PM", messageFormat.format(new Object[] {new Date(1564660800000L)}));
    }
}
