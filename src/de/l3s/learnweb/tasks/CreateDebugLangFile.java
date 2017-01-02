package de.l3s.learnweb.tasks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * 
 * @author Philipp
 *
 */
public class CreateDebugLangFile
{
    private static final Logger log = Logger.getLogger(CreateDebugLangFile.class);

    public static void main(String[] args) throws IOException
    {
        ResourceBundle bundle = ResourceBundle.getBundle("de.l3s.learnweb.lang.messages");
        Enumeration<String> keys = bundle.getKeys();

        FileOutputStream out = new FileOutputStream("Resources/de/l3s/learnweb/lang/messages_xx.properties");
        Properties props = new Properties();

        while(keys.hasMoreElements())
        {

            String key = keys.nextElement();
            props.setProperty(key, "#" + key + "#");
            log.debug(key);

        }

        props.store(out, null);
        out.close();

    }

}
