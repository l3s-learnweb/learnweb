package messages;

import java.text.MessageFormat;
import java.util.Date;

public class TestSingleMessage
{
    public static void main(String[] args)
    {
        // MessageFormat test = new MessageFormat("{0, choice, 0# 0|0< {0, number} ({1, number, 0.00}%)}");
        MessageFormat test = new MessageFormat("{0,date,medium} {0,time,short}");
        MessageFormat test2 = new MessageFormat("{0,date,MMM d, yyyy h:mm a}");

        Object[] testArgs = {new Date()};
        System.out.println(test.format(testArgs));
        System.out.println(test2.format(testArgs));
    }
}
