package de.l3s.searchHistoryTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpUtils
{
    /**
     * get the content of content (html source code) from given url
     *
     * @param baseUrl
     * @param headers
     * @param parameters
     * @return
     * @throws IOException
     */
    public static String GetContentByPost(String baseUrl, Map<String, String> headers, Map<String, String> parameters) throws IOException
    {
        try(CloseableHttpClient httpClient = HttpClients.createDefault())
        {
            HttpPost httpPost = new HttpPost(baseUrl);

            for(Map.Entry<String, String> header : headers.entrySet())
            {
                httpPost.setHeader(header.getKey(), header.getValue());
            }

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for(Map.Entry<String, String> parameter : parameters.entrySet())
            {
                nvps.add(new BasicNameValuePair(parameter.getKey(), parameter.getValue()));
            }

            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
            try(CloseableHttpResponse response = httpClient.execute(httpPost))
            {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            }
        }
    }

    public static void main(String args[]) throws IOException
    {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("text",
                "Diplomacy is the art and practice of conducting negotiations between representatives of states. It usually refers to international diplomacy, the conduct of international relations[2] through the intercession of professional diplomats with regard to a full range of topical issues. International treaties are usually negotiated by diplomats prior to endorsement by national politicians. David Stevenson reports that by 1900 the term \"diplomats\" also covered diplomatic services, consular services and foreign ministry officials.");
        parameters.put("language", "en");
        parameters.put("normalize", "true");
        parameters.put("depth", "0");
        String content = HttpUtils.GetContentByPost("http://tagtheweb.com.br/wiki/getFingerPrint.php", headers, parameters);
        System.out.println(content);
    }
}
