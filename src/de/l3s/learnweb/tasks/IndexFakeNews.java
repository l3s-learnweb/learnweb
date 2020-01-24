package de.l3s.learnweb.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;

@SuppressWarnings("unused")
public class IndexFakeNews
{
    private final static Logger log = Logger.getLogger(IndexFakeNews.class);

    private Learnweb learnweb;

    private ResourceManager resourceManager;

    private Resource logoResource;

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException
    {
        System.exit(-1);

        new IndexFakeNews();
    }

    public IndexFakeNews()
    {
        try
        {
            learnweb = Learnweb.createInstance();
            resourceManager = learnweb.getResourceManager();

            //indexFullfactFile("d:\\full_fact.csv");
            //reindexAllFakeNewsResources();
            //indexSnopes();

        }
        catch(Throwable e)
        {
            log.fatal(e);
        }
        finally
        {
            learnweb.onDestroy();
        }
    }

    private void indexFullfactFile(String file) throws FileNotFoundException, IOException, SQLException
    {
        CSVParser parser = CSVParser.parse(new File(file), StandardCharsets.UTF_8, CSVFormat.EXCEL.withHeader());

        logoResource = learnweb.getResourceManager().getResource(217749);

        for(CSVRecord csvRecord : parser)
        {
            String title = csvRecord.get("title").trim();
            String url = csvRecord.get("url").trim();
            String description = csvRecord.get("claim_text").trim().replaceFirst("Claim\n", "<b>Claim</b>: ") + "\n<br/>" + csvRecord.get("conclusion_text").trim().replaceFirst("Conclusion\n", "<b>Conclusion</b>: ");

            Resource resource = new Resource();
            resource.setType(ResourceType.website);
            resource.setSource(ResourceService.factcheck);
            resource.setLocation("FactCheck");
            resource.setMetadataValue("publisher", "fullfact.org");
            resource.setUserId(7727); // Admin
            resource.setGroupId(1346); // Admin Fact Check group

            resource.setThumbnail0(logoResource.getThumbnail0());
            resource.setThumbnail1(logoResource.getThumbnail1());
            resource.setThumbnail2(logoResource.getThumbnail2());
            resource.setThumbnail3(logoResource.getThumbnail3());
            resource.setThumbnail4(logoResource.getThumbnail4());

            resource.setTitle(title);
            resource.setDescription(description);
            resource.setUrl(url);

            resource.save();

            log.debug("Added resource: " + resource);
        }
    }

    public void reindexAllFakeNewsResources() throws SQLException
    {
        int counter = 0;
        List<Resource> resources = resourceManager.getResources("SELECT * FROM lw_resource r WHERE deleted = 0 AND group_id = 1346 AND source = ? and url like 'http://fullfa%'", "FactCheck");
        resourceManager.setReindexMode(true);
        SolrClient solrClient = learnweb.getSolrClient();
        for(Resource resource : resources)
        {
            log.debug("Process: " + counter++ + " - " + resource);
            solrClient.reIndexResource(resource);
            //resourceManager.deleteResource(resource.getId());
        }

        System.exit(0);
    }

    private void indexSnopes()
    {
        int i = 1;
        for(File file : new File("./Snopes").listFiles())
        {
            log.debug("process file: " + (i++) + ", " + file);
            indexSnopesFile(file);
        }
    }

    private void indexSnopesFile(File file)
    {
        try
        {
            Resource resource = new Resource();
            resource.setType(ResourceType.website);
            resource.setSource(ResourceService.factcheck);
            resource.setLocation("FactCheck");
            resource.setMetadataValue("publisher", "snopes.com");
            resource.setUserId(7727); // Admin
            resource.setGroupId(1346); // Admin Fact Check group

            JSONObject jsonObject = new JSONObject(new FileReader(file));

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
