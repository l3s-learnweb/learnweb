package messages;

import java.text.MessageFormat;
import java.util.Date;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class MessageFormatTest
{
    @Test
    void testFormatDate()
    {
        MessageFormat dateAndTime = new MessageFormat("{0,date,medium} {0,time,short}");
        MessageFormat customFormat = new MessageFormat("{0,date,MMM d, yyyy h:mm a}");

        Object[] testArgs = {new Date(1564660800000L)};
        assertEquals("Aug 1, 2019 2:00 PM", dateAndTime.format(testArgs));
        assertEquals("Aug 1, 2019 2:00 PM", customFormat.format(testArgs));
    }
}
