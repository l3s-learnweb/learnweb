package de.l3s.learnweb.resource.speechRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.user.User;

public class Speechrepository
{
    private static final Logger log = LogManager.getLogger(Speechrepository.class);

    public static void main(String[] args) throws Exception
    {
        new Speechrepository().importVideos();
    }

    private void importVideos() throws Exception
    {
        Learnweb learnweb = Learnweb.createInstance();
        ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
        Group group = learnweb.getGroupManager().getGroupById(1401);
        User admin = learnweb.getUserManager().getUser(7727);

        Pattern languagePattern = Pattern.compile("\\(([a-z]{2,3})\\)");
        Pattern datePattern = Pattern.compile("([^\\s]+)$");
        //Bruxelles, 09/04/2018
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        ResultSet rs = learnweb.getConnection().createStatement().executeQuery("SELECT * FROM speechrepository_video WHERE learnweb_resource_id = 0");
        while(rs.next())
        {
            // "rights" column not used
            PreparedStatement updateId = learnweb.getConnection().prepareStatement("UPDATE speechrepository_video SET learnweb_resource_id = ? WHERE id = ?");

            Resource resource = new Resource();
            resource.setTitle(rs.getString("title"));
            resource.setDescription(rs.getString("description") + "\\n<br/>\n" + rs.getString("notes"));
            resource.setUrl(rs.getString("url"));
            resource.setMaxImageUrl(rs.getString("image_link"));
            resource.setFileUrl(rs.getString("video_link"));
            resource.setSource(ResourceService.speechrepository);
            resource.setType(ResourceType.video);
            resource.setDuration(rs.getInt("duration"));
            resource.setIdAtService(rs.getString("id"));

            resource.setMetadataValue("language_level", rs.getString("level"));
            resource.setMetadataValue("use", rs.getString("use"));
            resource.setMetadataValue("type", rs.getString("type"));
            resource.setMetadataValue("domains", rs.getString("domains"));
            resource.setMetadataValue("terminology", rs.getString("terminology"));

            log.debug("process entry id " + resource.getIdAtService());

            // parse language
            Matcher matcher = languagePattern.matcher(rs.getString("language"));
            int matchCount = 0;
            String lang = null;
            while(matcher.find())
            {
                lang = matcher.group(1);
                matchCount++;
            }
            if(matchCount != 1)
                throw new RuntimeException("did not expect this lang value: " + rs.getString("language"));

            resource.setLanguage(lang);

            // parse date
            String dateStr = rs.getString("date");

            if(StringUtils.isNotBlank(dateStr))
            {
                matcher = datePattern.matcher(dateStr);
                if(matcher.find())
                    dateStr = matcher.group(1); // remove city from date string

                Date date = dateFormat.parse(dateStr);

                resource.setCreationDate(date);
            }

            //resource.setTranscript("");

            rpm.processImage(resource, FileInspector.openStream(resource.getMaxImageUrl()));
            resource.setGroup(group);
            admin.addResource(resource);
            //tedResource.save();

            //save new TED resource ID in order to use it later for saving transcripts

            updateId.setInt(1, resource.getId());
            updateId.setString(2, resource.getIdAtService());
            updateId.executeUpdate();
            updateId.close();
        }
        learnweb.onDestroy();
    }

}
