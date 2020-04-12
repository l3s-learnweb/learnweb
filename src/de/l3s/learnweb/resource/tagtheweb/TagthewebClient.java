package de.l3s.learnweb.resource.tagtheweb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.util.StringHelper;

public class TagthewebClient
{
    private static final Logger log = Logger.getLogger(TagthewebClient.class);

    private static String requestCategories(final String text, final String language) throws IOException
    {
        try(CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build())
        {
            HttpPost httpPost = new HttpPost("http://tagtheweb.com.br/wiki/getFingerPrint.php");
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("text", text));
            params.add(new BasicNameValuePair("language", language));
            params.add(new BasicNameValuePair("normalize", "true"));
            params.add(new BasicNameValuePair("depth", "0"));
            httpPost.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

            try(CloseableHttpResponse response = httpClient.execute(httpPost))
            {
                if(response.getStatusLine().getStatusCode() == 200)
                {
                    return EntityUtils.toString(response.getEntity());
                }

                String entityValue = EntityUtils.toString(response.getEntity());
                log.warn(response.getStatusLine().getReasonPhrase() + ": " + entityValue);
                return null;
            }
        }
    }

    public static Map<String, Double> getCategories(final String text, final String language) throws IOException
    {
        Map<String, Double> categories = new HashMap<>();
        String jsonResults = requestCategories(text, language);
        if(jsonResults != null)
        {
            JSONObject resultsJson = new JSONObject(jsonResults);
            for(String key : resultsJson.keySet())
            {
                double value = resultsJson.getDouble(key);
                if(value > 0.0d)
                {
                    categories.put(key, value);
                }
            }
            log.info("TagTheWeb returned " + resultsJson.length() + " categories (" + categories.size() + " non-zero) for text: " + text);
        }
        return categories;
    }

    public static Map<String, Double> getTopCategories(final String text, final String language) throws IOException
    {
        return getTopCategories(text, language, 5);
    }

    public static Map<String, Double> getTopCategories(final String text, final String language, final int limit) throws IOException
    {
        Map<String, Double> categories = getCategories(text, language);
        if(categories.size() <= 5)
            return categories;

        List<Map.Entry<String, Double>> entryList = new ArrayList<>(categories.entrySet());
        entryList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        return entryList.subList(0, limit).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException
    {
        Learnweb learnweb = Learnweb.createInstance();
        ResourceManager rm = new ResourceManager(learnweb);

        //List<Resource> resources = learnweb.getResourceManager().getResourcesByCourseId(505);
        List<Resource> resources = learnweb.getResourceManager().getResourcesByGroupId(1158);

        log.info("Total resources found " + resources.size());

        for(Resource resource : resources)
        {
            try
            {
                Map<String, Double> currentCategories = rm.getCategoriesByResource(resource.getId());

                if(currentCategories.isEmpty())
                {
                    if(StringUtils.isEmpty(resource.getMachineDescription()))
                    {
                        log.info("Starting getting resource metadata " + resource.getId());
                        learnweb.getResourceMetadataExtractor().processResource(resource);

                        log.info("New machine description retrieved: " + StringUtils.isNotEmpty(resource.getMachineDescription()));
                        resource.save();
                    }

                    if(StringUtils.isNotEmpty(resource.getMachineDescription()))
                    {
                        log.info("Getting categories of resource " + resource.getId());
                        String text = StringHelper.shortnString(resource.getMachineDescription(), 1000);

                        log.info("Retrieving categories from TagTheWeb");
                        Map<String, Double> categories = getCategories(text, "en");
                        log.info("Retrieved " + categories.size() + " categories.");
                        for(Map.Entry<String, Double> category : categories.entrySet())
                        {
                            rm.addCategory(resource, category.getKey(), category.getValue());
                        }

                        log.info(categories.size() + " categories were added to resource " + resource.getId());
                    }
                    else
                    {
                        log.warn("No description for resource " + resource.getId());
                    }
                }
            }
            catch(Exception e)
            {
                log.error("Error found ", e);
            }
        }

        System.exit(0);
    }
}
