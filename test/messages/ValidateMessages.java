package messages;

import java.text.MessageFormat;
import java.util.*;

public class ValidateMessages
{
    private static final List<String> LOCALES = Arrays.asList("de", "de_DE_AMA", "de_DE_Archive", "en", "en_UK_Archive", "it", "pt", "es", "uk");

    public static void main(String[] args)
    {
        for (String locale : LOCALES)
        {
            System.out.println("Reading messages for: " + locale);
            ResourceBundle bundle = ResourceBundle.getBundle("de/l3s/learnweb/lang/messages", Locale.forLanguageTag(locale));

            Map<String, String> messages = getMessagesFromBundle(bundle);
            validateMessages(messages);
        }
    }

    private static Map<String, String> getMessagesFromBundle(final ResourceBundle bundle)
    {
        final Map<String, String> messages = new HashMap<>();
        Collections.list(bundle.getKeys()).forEach(key -> messages.put(key, bundle.getString(key)));
        return messages;
    }

    private static void validateMessages(final Map<String, String> messages)
    {
        for (Map.Entry<String, String> message : messages.entrySet())
        {
            try
            {
                new MessageFormat(message.getValue());
            }
            catch(Exception e)
            {
                System.out.println(message.getKey() + " - the key is not valid!");
            }
        }
    }
}
