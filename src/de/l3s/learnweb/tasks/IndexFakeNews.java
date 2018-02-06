package de.l3s.learnweb.tasks;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.l3s.learnweb.Learnweb;

public class IndexFakeNews
{
    private final static Logger log = Logger.getLogger(IndexFakeNews.class);

    public static void main(String[] args) throws IOException
    {

    }

    public IndexFakeNews() throws IOException, ClassNotFoundException, SQLException
    {
        Learnweb learnweb = Learnweb.createInstance(null);

        for(File file : new File("C:\\Programmieren\\Snopes").listFiles())
        {
            System.out.println(file);
            indexFile(file);
            break;
        }
    }

    private static void indexFile(File file)
    {
        JSONParser parser = new JSONParser();

        try
        {
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(file));

            String Origins = (String) jsonObject.get("Origins");
            System.out.println(Origins);

            String title = (String) jsonObject.get("Fact Check");
            System.out.println(title);

            String description = (String) jsonObject.get("Description");
            description += "\n" + Origins;
            System.out.println(description);

            String machineDescription = (String) jsonObject.get("Example");
            System.out.println(machineDescription);

            String URL = (String) jsonObject.get("URL");
            if(!URL.startsWith("http"))
                URL = "http://" + URL;
            System.out.println(URL);

            String tags = (String) jsonObject.get("Tags");
            System.out.println(tags.split(";"));

            System.out.println(jsonObject);
        }
        catch(Exception e)
        {
            log.error(e);
        }
    }

}
