package de.l3s.interwebj;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.oauth.client.OAuthClientFilter;
import com.sun.jersey.oauth.signature.HMAC_SHA1;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;

public class InterWeb implements Serializable
{
    private static final Logger log = Logger.getLogger(InterWeb.class);
    private static final long serialVersionUID = -1621494088505203391L;

    private final String consumerKey;
    private final String consumerSecret;
    private final String interwebApiURL;

    private AuthCredentials iwToken = null;

    public InterWeb(String interwebApiURL, String consumerKey, String consumerSecret)
    {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.interwebApiURL = interwebApiURL;
    }

    public String getConsumerKey()
    {
        return consumerKey;
    }

    public String getConsumerSecret()
    {
        return consumerSecret;
    }

    public String getInterwebApiURL()
    {
        return interwebApiURL;
    }

    public AuthCredentials getIWToken()
    {
        return iwToken;
    }

    public void setIWToken(AuthCredentials iwToken)
    {
        this.iwToken = iwToken;
    }

    private WebResource createWebResource(String apiPath, AuthCredentials userAuthCredentials)
    {
        String apiUrl = getInterwebApiURL() + apiPath;
        AuthCredentials consumerAuthCredentials = new AuthCredentials(getConsumerKey(), getConsumerSecret());

        Client client = Client.create();
        WebResource resource = client.resource(apiUrl);
        OAuthParameters oauthParams = new OAuthParameters();
        oauthParams.consumerKey(consumerAuthCredentials.getKey());
        if(userAuthCredentials != null)
        {
            oauthParams.token(userAuthCredentials.getKey());
        }
        oauthParams.signatureMethod(HMAC_SHA1.NAME);
        oauthParams.timestamp();
        oauthParams.nonce();
        oauthParams.version();
        OAuthSecrets oauthSecrets = new OAuthSecrets();
        oauthSecrets.consumerSecret(consumerAuthCredentials.getSecret());
        if(userAuthCredentials != null && userAuthCredentials.getSecret() != null)
        {
            oauthSecrets.tokenSecret(userAuthCredentials.getSecret());
        }
        OAuthClientFilter filter = new OAuthClientFilter(client.getProviders(), oauthParams, oauthSecrets);
        resource.addFilter(filter);

        log.debug("created WebResource: " + resource.toString());
        return resource;
    }

    //------------------------------------------------------------------------------------

    /**
     * Creates a new instance of interweb with the same consumer key and secret
     */
    @Override
    public InterWeb clone()
    {
        return new InterWeb(getInterwebApiURL(), getConsumerKey(), getConsumerSecret());
    }

    /**
     *
     * @param query The query string
     * @param params see http://athena.l3s.uni-hannover.de:8000/doc/search
     * @return
     * @throws IOException
     * @throws IllegalResponseException
     */
    public SearchQuery search(String query, TreeMap<String, String> params) throws IOException, IllegalResponseException
    {
        if(null == query || query.length() == 0)
        {
            throw new IllegalArgumentException("empty query");
        }

        WebResource resource = createWebResource("search", getIWToken());
        resource = resource.queryParam("q", query);
        for(String key : params.keySet())
        {
            String value = params.get(key);
            resource = resource.queryParam(key, value);
        }

        ClientResponse response = resource.get(ClientResponse.class);

        if(response.getStatus() != 200)
        {
            String content = responseToString(response);

            log.fatal("Interweb request failed; Error code : " + response.getStatus() + "; for query:" + query + " | " + params + "; response: " + content);
            throw new RuntimeException("Interweb request failed : HTTP error code : " + response.getStatus());
        }

        //SearchResponse r = response.getEntity(SearchResponse.class);

        return new SearchQuery(response.getEntityInputStream()); //.getResults();
    }

    public static String responseToString(ClientResponse response)
    {
        StringWriter writer = new StringWriter();
        try
        {
            IOUtils.copy(response.getEntityInputStream(), writer, "UTF-8");
        }
        catch(IOException e)
        {
            log.warn("Can't convert response to String", e);
            return null;
        }
        return writer.toString();
    }

    public static void main(String[] args) throws Exception
    {
        TreeMap<String, String> params = new TreeMap<>();

        params.put("media_types", "video");
        //params.put("services", "YouTube,Vimeo");
        params.put("number_of_results", "8");
        params.put("page", "1");

        InterWeb iw = new InterWeb("http://learnweb.l3s.uni-hannover.de/interweb/api/", "***REMOVED***", "***REMOVED***");

        SearchQuery interwebResponse = iw.search("london", params);
        interwebResponse.getResults();

    }
}
