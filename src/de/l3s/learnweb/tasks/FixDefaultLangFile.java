package de.l3s.learnweb.tasks;

import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * the default and englisch lang file should be identical. This script checks for differences
 *
 * @author Philipp
 *
 */
public class FixDefaultLangFile
{
    private static final Logger log = Logger.getLogger(FixDefaultLangFile.class);

    public static void main(String[] args) throws Exception
    {
        new FixDefaultLangFile();
    }

    public FixDefaultLangFile() throws Exception
    {
        Properties langDefault = new Properties();
        langDefault.load(getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/lang/messages.properties"));

        Properties langEn = new Properties();
        langEn.load(getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/lang/messages_en_UK.properties"));

        Set<Object> keys = langEn.keySet();

        for(Object obj : keys)
        {
            String key = (String) obj;
            String valueDefauult = langDefault.getProperty(key);
            String valueEn = langEn.getProperty(key);

            if(StringUtils.isEmpty(valueDefauult))
            {
                log.debug(key + " empty");
                langDefault.setProperty(key, valueEn);
            }
            else if(!valueDefauult.equals(valueEn))
            {
                log.debug(key + " diff: " + valueDefauult + " || " + valueEn);
                langDefault.setProperty(key, valueEn);
            }
        }

        FileOutputStream out = new FileOutputStream("Resources/de/l3s/learnweb/lang/messages_xx.properties");
        langDefault.store(out, null);
        out.close();
    }
}
