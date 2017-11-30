package de.l3s.learnweb.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.ResourcePreviewMaker;

public class UpdateTedResources
{
    private static Logger log = Logger.getLogger(UpdateTedResources.class);

    private final static String VIMEO_PATTERN = "https?://(?:www\\.)?(?:player\\.)?vimeo\\.com/(?:[a-z]*/)*([0-9]{6,11})[?]?.*";

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     * @throws MalformedURLException
     */
    public static void main(String[] args) throws SQLException, MalformedURLException, IOException, ClassNotFoundException
    {
        //System.out.println(AddResourceBean.checkUrl("http://www.teachingideas.co.uk/"));
        //System.exit(0);

        Learnweb learnweb = Learnweb.createInstance(null);
        ResourceManager rm = learnweb.getResourceManager();
        ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();

        String query = "SELECT * FROM `lw_resource` WHERE deleted =0 and source like 'ted'"; // thumbnail0_file_id = 0 ORDER BY resource_id DESC";

        List<Resource> resources = rm.getResources(query, null);
        log.debug("start");
        for(Resource resource : resources)
        {
            /*
            Pattern compVimeoPattern = Pattern.compile(VIMEO_PATTERN, Pattern.CASE_INSENSITIVE);
            Matcher vimeoMatcher = compVimeoPattern.matcher(resource.getUrl());
            if(!vimeoMatcher.find())
            {
                log.warn("Can't identify id for resource: " + resource.getId());
                continue;
            }*/

            // TODO
            //  resource.setIdAtService(vimeoMatcher.group(1));
            resource.setEmbeddedRaw(null);
            resource.setUrl(resource.getUrl().replace("http://", "https://"));

            resource.save();

        }
        log.debug("done");

        learnweb.onDestroy();
    }

}
