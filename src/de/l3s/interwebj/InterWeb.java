package de.l3s.interwebj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.oauth.client.OAuthClientFilter;
import com.sun.jersey.oauth.signature.HMAC_SHA1;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;

import de.l3s.interwebj.AuthorizationInformation.ServiceInformation;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.beans.UtilBean;

public class InterWeb implements Serializable
{
    private static final long serialVersionUID = -1621494088505203391L;

    private final String consumerKey;
    private final String consumerSecret;
    private final String interwebApiURL;

    private int usernameLastCacheTime = 0;
    private int authorizationInformationLastCacheTime = 0;

    private String username;
    private AuthorizationInformation authorizationInformation;

    private AuthCredentials iwToken = null;
    private int serviceInformationListCacheTime;
    private List<ServiceInformation> serviceInformationListCache;

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

    private void resetAuthorizationCache()
    {
	authorizationInformationLastCacheTime = 0;
    }

    private void resetUsernameCache()
    {
	usernameLastCacheTime = 0;
    }

    public void setIWToken(AuthCredentials iwToken)
    {
	this.iwToken = iwToken;
	// force to reload
	resetAuthorizationCache();
	resetUsernameCache();
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

	return resource;
    }

    private WebResource createPublicWebResource(String apiPath)
    {
	return createWebResource(apiPath, null);
    }

    public synchronized List<ServiceInformation> getServiceInformation(boolean useCache) throws IllegalResponseException
    {

	if(serviceInformationListCacheTime < UtilBean.time() - 86500)
	{
	    WebResource resource = createPublicWebResource("services");

	    ClientResponse response = resource.get(ClientResponse.class);
	    AuthorizationInformation temp = new AuthorizationInformation(response.getEntityInputStream());

	    serviceInformationListCache = temp.getServices();
	    serviceInformationListCacheTime = UtilBean.time();
	}
	return serviceInformationListCache;
    }

    //------------------------------------------------------------------------------------

    public void authorizeService(ServiceInformation service, String callback) throws IllegalResponseException
    {
	resetAuthorizationCache();

	WebResource resource = createWebResource("users/default/services/" + service.getId() + "/auth", getIWToken());
	resource = resource.queryParam("callback", callback);
	MultivaluedMap<String, String> params = new MultivaluedMapImpl();
	if(service.getKey() != null && service.getSecret() != null)
	{
	    params.add("username", service.getKey());
	    params.add("password", service.getSecret());
	}
	ClientResponse response = resource.type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, params);
	//CoreUtils.printClientResponse(response);
	Element root = asXML(response);
	if(!root.attribute("stat").getValue().equals("ok"))
	{
	    throw new IllegalResponseException(root.asXML());
	}
	String link = root.element("link").getStringValue();

	System.out.println("redirecting to: [" + link + "]");
	UtilBean.redirect(link);
    }

    /**
     * Creates a new instance of interweb with the same consumer key and secret
     */
    @Override
    public InterWeb clone()
    {
	return new InterWeb(getInterwebApiURL(), getConsumerKey(), getConsumerSecret());
    }

    public void deleteToken()
    {
	setIWToken(null);
    }

    public AuthCredentials getAccessToken(AuthCredentials authCredentials) throws IllegalResponseException
    {
	WebResource resource = createWebResource("oauth/OAuthGetAccessToken", authCredentials);
	ClientResponse response = resource.get(ClientResponse.class);
	Element root = asXML(response);
	if(!root.attribute("stat").getValue().equals("ok"))
	{
	    throw new IllegalResponseException(root.asXML());
	}
	Element element = root.element("access_token");
	String token = element.element("oauth_token").getStringValue();
	String tokenSecret = element.element("oauth_token_secret").getStringValue();
	return new AuthCredentials(token, tokenSecret);
    }

    public synchronized AuthorizationInformation getAuthorizationInformation(boolean useCache) throws IOException, IllegalResponseException
    {
	if(null == getIWToken())
	    return null;

	if(!useCache || authorizationInformationLastCacheTime < UtilBean.time() - 6000)
	{
	    WebResource resource = createWebResource("users/default/services", getIWToken());
	    ClientResponse response = resource.get(ClientResponse.class);

	    authorizationInformation = new AuthorizationInformation(response.getEntityInputStream());
	    authorizationInformationLastCacheTime = UtilBean.time();
	}
	return authorizationInformation;
    }

    public String getAuthorizeUrl(String callback) throws IllegalResponseException
    {
	System.out.println("callback: [" + callback + "]");
	WebResource resource = createPublicWebResource("oauth/OAuthGetRequestToken");
	ClientResponse response = resource.get(ClientResponse.class);
	Element root = asXML(response);
	if(!root.attribute("stat").getValue().equals("ok"))
	{
	    throw new IllegalResponseException(root.asXML());
	}
	String iw_token = root.element("request_token").element("oauth_token").getStringValue();
	return getInterwebApiURL() + "oauth/OAuthAuthorizeToken" + "?oauth_token=" + iw_token + "&oauth_callback=" + callback;
    }

    public synchronized String getUsername() throws IllegalResponseException
    {
	if(null == getIWToken())
	    return null;

	if(usernameLastCacheTime < UtilBean.time() - 6000) // cached value older then 100 minutes
	{
	    WebResource resource = createWebResource("users/default", getIWToken());
	    ClientResponse response = resource.get(ClientResponse.class);

	    try
	    {
		username = getClientResponseContent(response);
		usernameLastCacheTime = UtilBean.time();
	    }
	    catch(IOException e)
	    {
		throw new IllegalResponseException(e);
	    }
	}
	return username;
    }

    public static String getClientResponseContent(ClientResponse response) throws IOException
    {
	StringBuilder sb = new StringBuilder();
	InputStream is = response.getEntityInputStream();
	BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
	int c;
	while((c = br.read()) != -1)
	{
	    sb.append((char) c);
	}
	br.close();
	return sb.toString();
    }

    /**
     * Registers a new user at interweb and returns his interweb_token
     * 
     * @param username
     * @param password
     * @param defaultToken Returns null if username is already taken
     * @return
     * @throws IllegalResponseException
     */
    public AuthCredentials registerUser(String username, String password, String defaultUserName, String defaultPassword) throws IllegalResponseException
    {
	MultivaluedMapImpl params = new MultivaluedMapImpl();
	params.add("username", username);
	params.add("password", password);
	if(StringUtils.isNotEmpty(defaultUserName))
	{
	    params.add("mediator_username", defaultUserName);
	}
	if(StringUtils.isNotEmpty(defaultPassword))
	{
	    params.add("mediator_password", defaultPassword);
	}
	WebResource resource = createPublicWebResource("oauth/register");
	ClientResponse response = resource.type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, params);
	Element root;
	try
	{
	    root = asXML(response);

	    Element element = root.element("access_token");
	    String token = element.element("oauth_token").getStringValue();
	    String tokenSecret = element.element("oauth_token_secret").getStringValue();
	    return new AuthCredentials(token, tokenSecret);
	}
	catch(IllegalResponseException e)
	{
	    if(e.getMessage().contains("User already exists"))
		return null;
	    throw e;
	}
    }

    public void revokeAuthorizationOnService(String serviceId) throws IOException, IllegalResponseException
    {
	WebResource resource = createWebResource("users/default/services/" + serviceId + "/auth", getIWToken());
	ClientResponse response = resource.delete(ClientResponse.class);
	Element root = asXML(response);
	System.out.println("revokeAuthorizationOnService" + root.asXML());
	//force reload
	resetAuthorizationCache();
    }

    private Element asXML(ClientResponse response) throws IllegalResponseException
    {
	Document doc;
	try
	{
	    doc = new SAXReader().read(response.getEntityInputStream());
	}
	catch(Exception e)
	{
	    throw new IllegalResponseException(e.getMessage());
	}
	Element root = doc.getRootElement();
	if(!root.attributeValue("stat").equals("ok"))
	{
	    throw new IllegalResponseException(root.asXML());
	}
	return root;
    }

    /*
    	private void convertMediaTypesParams(TreeMap<String, String> params)
    	{
    		Set<String> newMediaTypes = new TreeSet<String>();
    		String mediaTypeParam = params.get("media_types");
    		if (mediaTypeParam == null)
    		{
    			return;
    		}
    		String[] mediaTypes = mediaTypeParam.split(",");
    		for (String mediaType : mediaTypes)
    		{
    			if (mediaType.equals("photos"))
    			{
    				newMediaTypes.add(Query.CT_IMAGE);
    			}
    			else if (mediaType.equals("videos"))
    			{
    				newMediaTypes.add(Query.CT_VIDEO);
    			}
    			else if (mediaType.equals("slideshows"))
    			{
    				newMediaTypes.add(Query.CT_IMAGE);
    			}
    			else if (mediaType.equals("audio"))
    			{
    				newMediaTypes.add(Query.CT_AUDIO);
    			}
    			else if (mediaType.equals("music"))
    			{
    				newMediaTypes.add(Query.CT_AUDIO);
    			}
    			else if (mediaType.equals("bookmarks"))
    			{
    				newMediaTypes.add(Query.CT_TEXT);
    			}
    		}
    		params.put("media_types", StringUtils.join(newMediaTypes, ','));
    	}*/

    public String buildSignature(String string, TreeMap<String, String> params)
    {
	System.err.println("Interweb.buildSignature hat olex nicht implementiert");
	return null;
    }

    // Überarbeitet:

    /**
     * 
     * @param query The query string
     * @param params see http://athena.l3s.uni-hannover.de:8000/doc/search
     * @return
     * @throws IOException
     * @throws IllegalResponseException
     */
    public List<ResourceDecorator> search(String query, TreeMap<String, String> params) throws IOException, IllegalResponseException
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
	//System.out.println(CoreUtils.getClientResponseContent(response));

	if(response.getStatus() != 200)
	{
	    throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
	}

	//SearchResponse r = response.getEntity(SearchResponse.class);

	return new SearchQuery(response.getEntityInputStream()).getResults();
    }

    public static void main(String[] args) throws Exception
    {
	TreeMap<String, String> params = new TreeMap<String, String>();

	params.put("media_types", "video");
	params.put("services", "YouTube,Vimeo");
	params.put("number_of_results", "8");
	params.put("page", "1");

	InterWeb iw = new InterWeb("http://learnweb.l3s.uni-hannover.de/interweb/api/", "***REMOVED***", "***REMOVED***");
	iw.search("london", params);
    }

    /**
     * 
     * @param resource
     * @param selectedUploadServices the services to which the resource should be uploaded
     * @return
     * @throws IllegalResponseException
     */
    public Resource upload(Resource resource, List<String> selectedUploadServices) throws IllegalResponseException
    {
	if(selectedUploadServices != null)
	    for(String s : selectedUploadServices)
		System.out.println(s);

	@SuppressWarnings("resource")
	MultiPart multiPart = new MultiPart();
	multiPart = multiPart.bodyPart(new FormDataBodyPart("title", resource.getTitle()));
	multiPart = multiPart.bodyPart(new FormDataBodyPart("description", resource.getDescription()));
	multiPart = multiPart.bodyPart(new FormDataBodyPart("content_type", resource.getType().toLowerCase()));
	multiPart = multiPart.bodyPart(new FileDataBodyPart("data", resource.getFile(4).getActualFile(), MediaType.MULTIPART_FORM_DATA_TYPE));
	multiPart = multiPart.bodyPart(new FormDataBodyPart("data", "the data"));

	WebResource webResource = createWebResource("users/default/uploads", getIWToken());
	WebResource.Builder builder = webResource.type(MediaType.MULTIPART_FORM_DATA);
	builder = builder.accept(MediaType.APPLICATION_XML);

	ClientResponse response = builder.post(ClientResponse.class, multiPart);

	try
	{
	    multiPart.close();
	}
	catch(IOException e)
	{
	}
	/*
	try
	{
		CoreUtils.printClientResponse(response);
		System.out.println(CoreUtils.getClientResponseContent(response));
	}
	catch (IOException e)
	{
		e.printStackTrace();
	}*/

	UploadResponse uploadResponse = new UploadResponse(response.getEntityInputStream());
	return uploadResponse.getResult();

    }
}
