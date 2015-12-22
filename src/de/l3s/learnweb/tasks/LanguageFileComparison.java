package de.l3s.learnweb.tasks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

/**
 * Uses the list of corrected translations provided by Marco to check which translations were not yet checked.
 * 
 * The returned list represents translation keys which were noch checked.
 * 
 * @author Philipp
 *
 */
public class LanguageFileComparison
{

    public static void main(String[] args) throws Exception
    {
	Properties languageProperties = new Properties();
	languageProperties.load(new LanguageFileComparison().getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/lang/messages.properties"));

	String csvFile = "test.csv";

	BufferedReader buffer = new BufferedReader(new FileReader(csvFile));
	String line;
	while((line = buffer.readLine()) != null)
	{

	    if(!line.startsWith("#") && line.endsWith("#"))
	    {
		System.err.println("Corrupted file at line: " + line);
		continue;
	    }

	    line = line.substring(1, line.length() - 1); // remove "#"

	    String translation = languageProperties.getProperty(line);

	    System.out.println(line);

	    if(translation == null)
	    {
		System.err.println("Illegal key: " + line);
		continue;
	    }

	    languageProperties.remove(line);
	}

	System.out.println("unchecked keys:");
	for(Object key : languageProperties.keySet())
	{
	    String keyStr = (String) key;
	    if(keyStr.startsWith("ArchiveSearch.") || keyStr.startsWith("language_"))
		continue;

	    System.out.println(key);
	}

	buffer.close();
    }
}
