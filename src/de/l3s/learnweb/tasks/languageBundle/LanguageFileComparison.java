package de.l3s.learnweb.tasks.languageBundle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Uses the list of corrected translations provided by Marco to check which translations were not yet checked.
 * 
 * The returned list represents translation keys which were not checked.
 * 
 * @author Philipp
 *
 */
public class LanguageFileComparison
{
    private static final Logger log = LogManager.getLogger(LanguageFileComparison.class);

    public static void main(String[] args) throws Exception
    {
        Properties languageProperties = new Properties();
        languageProperties.load(LanguageFileComparison.class.getClassLoader().getResourceAsStream("de/l3s/learnweb/lang/messages.properties"));

        String csvFile = "test.csv";

        BufferedReader buffer = new BufferedReader(new FileReader(csvFile));
        String line;
        while((line = buffer.readLine()) != null)
        {

            if(!line.startsWith("#") && line.endsWith("#"))
            {
                log.error("Corrupted file at line: " + line);
                continue;
            }

            line = line.substring(1, line.length() - 1); // remove "#"

            String translation = languageProperties.getProperty(line);

            log.debug(line);

            if(translation == null)
            {
                log.error("Illegal key: " + line);
                continue;
            }

            languageProperties.remove(line);
        }

        log.debug("unchecked keys:");
        for(Object key : languageProperties.keySet())
        {
            String keyStr = (String) key;
            if(keyStr.startsWith("ArchiveSearch.") || keyStr.startsWith("language_"))
                continue;

            log.debug(key);
        }

        buffer.close();
    }
}
