package de.l3s.maintenance.resources;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;

@SuppressWarnings("unused")
public class IndexFakeNews {
    private static final Logger log = LogManager.getLogger(IndexFakeNews.class);

    private Learnweb learnweb;

    private ResourceManager resourceManager;

    private Resource logoResource;

    public IndexFakeNews() {
        try {
            learnweb = Learnweb.createInstance();
            resourceManager = learnweb.getResourceManager();

            //indexFullfactFile("d:\\full_fact.csv");
            //reindexAllFakeNewsResources();
            //indexSnopes();

        } catch (Throwable e) {
            log.fatal(e);
        } finally {
            learnweb.onDestroy();
        }
    }

    private void indexFullfactFile(String file) throws IOException, SQLException {
        CSVParser parser = CSVParser.parse(new File(file), StandardCharsets.UTF_8, CSVFormat.EXCEL.withHeader());

        logoResource = learnweb.getResourceManager().getResource(217749);

        for (CSVRecord csvRecord : parser) {
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

    public void reindexAllFakeNewsResources() throws SQLException {
        int counter = 0;
        List<Resource> resources = resourceManager.getResources("SELECT * FROM lw_resource r WHERE deleted = 0 AND group_id = 1346 AND source = ? and url like 'http://fullfa%'", "FactCheck");
        resourceManager.setReindexMode(true);
        SolrClient solrClient = learnweb.getSolrClient();
        for (Resource resource : resources) {
            log.debug("Process: " + counter++ + " - " + resource);
            solrClient.reIndexResource(resource);
            //resourceManager.deleteResource(resource.getId());
        }
    }

    private void indexSnopes() throws JsonIOException, JsonSyntaxException, IOException, SQLException {
        File[] files = new File("./Snopes").listFiles();
        if (files != null) {
            int i = 1;
            for (File file : files) {
                log.debug("process file: {}, {}", i++, file);
                indexSnopesFile(file);
            }
        }
    }

    private void indexSnopesFile(File file) throws JsonIOException, JsonSyntaxException, IOException, SQLException {
        Resource resource = new Resource();
        resource.setType(ResourceType.website);
        resource.setSource(ResourceService.factcheck);
        resource.setLocation("FactCheck");
        resource.setMetadataValue("publisher", "snopes.com");
        resource.setUserId(7727); // Admin
        resource.setGroupId(1346); // Admin Fact Check group

        JsonObject jsonObject = JsonParser.parseReader(new FileReader(file, StandardCharsets.UTF_8)).getAsJsonObject();

        String title = jsonObject.get("Fact Check").getAsString();
        if (StringUtils.isEmpty(title)) {
            title = jsonObject.get("Claim").getAsString();
        }
        if (StringUtils.isEmpty(title)) {
            title = jsonObject.get("Claim_ID").getAsString().replace("-", " ");
        }
        resource.setTitle(title);

        String origins = jsonObject.get("Origins").getAsString();
        String description = jsonObject.get("Description").getAsString();
        description += "\n" + origins;
        resource.setDescription(description);

        String machineDescription = jsonObject.get("Example").getAsString();
        resource.setMachineDescription(machineDescription);

        String url = jsonObject.get("URL").getAsString();
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        resource.setUrl(url);

        resource.save();

        log.debug("Added resource: " + resource);
        /*
        String tags = jsonObject.get("Tags").getAsString();
        for(String tag : tags.split(";"))
            resource.addTag(tag, user);
        */

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        System.exit(-1);

        new IndexFakeNews();
    }

}