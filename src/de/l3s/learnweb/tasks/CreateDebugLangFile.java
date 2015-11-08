package de.l3s.learnweb.tasks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * 
 * @author Philipp
 *
 */
public class CreateDebugLangFile
{

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
	    System.out.println(key);

	}

	props.store(out, null);
	out.close();

    }

}
