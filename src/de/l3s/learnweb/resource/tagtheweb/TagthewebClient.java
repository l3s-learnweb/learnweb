package de.l3s.learnweb.resource.tagtheweb;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.util.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class TagthewebClient
{
    private final static Logger log = Logger.getLogger(TagthewebClient.class);

    private static String requestCategories(String text, String language) throws IOException
    {
        try(CloseableHttpClient httpClient = HttpClients.createDefault())
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
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }

                String entityValue = EntityUtils.toString(response.getEntity());
                log.warn(response.getStatusLine().getReasonPhrase() + ": " + entityValue);
                return null;
            }
        }
    }

    public static Map<String, Double> getCategories(String text, String language) throws IOException
    {
        Map<String, Double> categories = new HashMap<>();
        String jsonResults = requestCategories(text, language);
        if (jsonResults != null) {
            JSONObject resultsJson = new JSONObject(jsonResults);
            for (String key : resultsJson.keySet()) {
                Double value = resultsJson.getDouble(key);
                categories.put(key, value);
            }
            log.info("TagTheWeb returned " + categories.size() + " categories for text: " + text);
        }
        return categories;
    }

    public static Map<String, Double> getTopCategories(String text, String language) throws IOException
    {
        Map<String, Double> categories = getCategories(text, language);

        if (categories.size() <= 5) {
            return categories;
        }

        Map<String, Double> selCategories = categories.entrySet().stream()
                .filter(a -> a.getValue() > .5d)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (selCategories.size() > 5) {
            return selCategories;
        }

        List<Map.Entry<String, Double>> entryList = new ArrayList<>(categories.entrySet());
        entryList.sort(Comparator.comparingDouble(Map.Entry::getValue));

        return entryList.subList(0, 5).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, Double> getNonZeroCategories(String text, String language) throws IOException
    {
        Map<String, Double> categories = getCategories(text, language);

        return categories.entrySet().stream()
                .filter(a -> a.getValue() > .0D)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static void main(String args[]) throws SQLException, ClassNotFoundException
    {
        Learnweb learnweb = Learnweb.createInstance("https://learnweb.l3s.uni-hannover.de");
        ResourceManager rm = new ResourceManager(learnweb);

        List<Resource> resources = learnweb.getResourceManager().getResourcesByCourseId(505);
        // List<Resource> resources = learnweb.getResourceManager().getResourcesByGroupId(1158);

        log.info("Total resources found " + resources.size());

        for(Resource resource : resources)
        {
            try {
                Map<String, Double> currentCategories = rm.getCategoriesByResource(resource.getId());

                if (currentCategories.size() == 0) {
                    if (StringUtils.isEmpty(resource.getMachineDescription())) {
                        log.info("Starting getting resource metadata " + resource.getId());
                        learnweb.getResourceMetadataExtractor().processResource(resource);

                        log.info("New machine description retrieved: " + StringUtils.isNotEmpty(resource.getMachineDescription()));
                        resource.save();
                    }

                    if (StringUtils.isNotEmpty(resource.getMachineDescription())) {
                        log.info("Getting categories of resource " + resource.getId());
                        String text = StringHelper.shortnString(resource.getMachineDescription(), 1000);

                        log.info("Retrieving categories from TagTheWeb");
                        Map<String, Double> categories = TagthewebClient.getNonZeroCategories(text, "en");
                        log.info("Retrieved " + categories.size() + " categories.");
                        for (Map.Entry<String, Double> category : categories.entrySet()) {
                            rm.addCategory(resource, category.getKey(), category.getValue());
                        }

                        log.info(categories.size() + " categories were added to resource " + resource.getId());
                    } else {
                        log.warn("No description for resource " + resource.getId());
                    }
                }
            } catch(Exception e) {
                log.error("Error found ", e);
            }
        }

        System.exit(0);
    }
}
