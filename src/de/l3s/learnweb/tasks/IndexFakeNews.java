package de.l3s.learnweb.tasks;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Resource.ResourceType;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.solrClient.SolrClient;

public class IndexFakeNews
{
    private final static Logger log = Logger.getLogger(IndexFakeNews.class);

    private Learnweb learnweb;

    private ResourceManager resourceManager;

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException
    {
        new IndexFakeNews();
    }

    public IndexFakeNews() throws IOException, ClassNotFoundException, SQLException
    {
        learnweb = Learnweb.createInstance(null);
        resourceManager = learnweb.getResourceManager();

        reindexAllFakeNewsResources();

        int i = 1;
        for(File file : new File("./Snopes").listFiles())
        {
            log.debug("process file: " + (i++) + ", " + file);
            indexFile(file);
        }
    }

    public void reindexAllFakeNewsResources() throws SQLException
    {
        List<Resource> resources = resourceManager.getResources("SELECT * FROM lw_resource r WHERE source = ?", "FactCheck");
        resourceManager.setReindexMode(true);
        SolrClient solrClient = learnweb.getSolrClient();
        for(Resource resource : resources)
        {
            log.debug("Process: " + resource);
            solrClient.reIndexResource(resource);
            //resourceManager.deleteResource(resource.getId());
        }

        System.exit(0);
    }

    private void indexFile(File file)
    {
        JSONParser parser = new JSONParser();

        try
        {
            Resource resource = new Resource();
            resource.setType(ResourceType.website);
            resource.setSource("FactCheck");
            resource.setLocation("FactCheck");
            resource.setMetadataValue("publisher", "snopes.com");
            resource.setUserId(7727); // Admin
            resource.setGroupId(1346); // Admin Fact Check group

            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(file));

            String title = (String) jsonObject.get("Fact Check");
            if(StringUtils.isEmpty(title))
                title = (String) jsonObject.get("Claim");
            if(StringUtils.isEmpty(title))
                title = ((String) jsonObject.get("Claim_ID")).replace("-", " ");
            resource.setTitle(title);

            String Origins = (String) jsonObject.get("Origins");
            String description = (String) jsonObject.get("Description");
            description += "\n" + Origins;
            resource.setDescription(description);

            String machineDescription = (String) jsonObject.get("Example");
            resource.setMachineDescription(machineDescription);

            String URL = (String) jsonObject.get("URL");
            if(!URL.startsWith("http"))
                URL = "http://" + URL;
            resource.setUrl(URL);

            resource.save();

            log.debug("Added resource: " + resource);
            /*
            String tags = (String) jsonObject.get("Tags");
            for(String tag : tags.split(";"))
                resource.addTag(tag, user);
            */
        }
        catch(Exception e)
        {
            log.error(e);
        }
    }

}
