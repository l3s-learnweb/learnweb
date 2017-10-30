package de.l3s.searchlogclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.searchlogclient.Actions.ACTION;
import de.l3s.searchlogclient.jaxb.CommentonSearch;
import de.l3s.searchlogclient.jaxb.HistoryByDate;
import de.l3s.searchlogclient.jaxb.HistoryByDateList;
import de.l3s.searchlogclient.jaxb.QueryLog;
import de.l3s.searchlogclient.jaxb.Resource;
import de.l3s.searchlogclient.jaxb.ResourceList;
import de.l3s.searchlogclient.jaxb.ResourceLog;
import de.l3s.searchlogclient.jaxb.ResourceLogList;
import de.l3s.searchlogclient.jaxb.ResultSetIdList;
import de.l3s.searchlogclient.jaxb.SearchCommentsList;
import de.l3s.searchlogclient.jaxb.ServiceResponse;
import de.l3s.searchlogclient.jaxb.SharedResultset;
import de.l3s.searchlogclient.jaxb.SharedResultsetList;
import de.l3s.searchlogclient.jaxb.Tag;
import de.l3s.searchlogclient.jaxb.TagList;
import de.l3s.searchlogclient.jaxb.UrlList;
import de.l3s.searchlogclient.jaxb.ViewingTime;
import de.l3s.searchlogclient.jaxb.ViewingTimeList;
import de.l3s.util.StringHelper;

public class SearchLogClient
{
    private static final Logger log = Logger.getLogger(SearchLogClient.class);

    //searchLog service URLs
    private static String baseURL;
    private static String queryLogURL = "querylog";
    private static String searchCommentURL = "searchcomment";
    private static String shareResultsetURL = "shareresultset/";
    private static String posttagListURL = "posttaglist";
    private static String addtagURL = "addtag";
    private static String deletetagURL = "deletetag/";
    private static String tagsByUserIdURL = "tagsbyuserid/";
    private static String sharedResultsetsByUserIdURL = "sharedresultsetsbyuserid/";
    private static String resultsetIdFromMd5URL = "resultsetidfromMd5/";
    private static String tagsByResultsetIdURL = "tagsbyresultsetid/";
    private static String resultsetLogURL = "xmlresultsetlog";
    private static String batchResultsetLogURL = "xmlbatchresultsetlog";
    private static String resourceLogURL = "resourcelog";
    private static String updateResultsetURL = "updateresultset";
    private static String viewingTimeURL = "updateviewingtimelog";
    private static String batchViewingTimeURL = "updatebatchviewingtimelog";
    private static String truncateTablesURL = "truncatetables";
    private static String removeUserURL = "removeuserentries/";
    private static String deleteUserQueriesURL = "deleteuserqueries";
    private static String searchHistoryByDateURL = "searchhistorybydate/";
    private static String searchHistoryByPagesURL = "searchhistorybypages/";
    private static String searchHistoryByQueryURL = "searchhistorybyquery/";
    private static String resourcesByQueryURL = "resourcesbyquery/";
    private static String resourceUrlsByResultsetIdURL = "resourceurlsbyresultsetid/";
    private static String resourcesByQueryAndTimestampURL = "resourcesbyqueryandtimestamp/";
    private static String resourcesByResultSetIdURL = "resourcesbyresultsetid/";
    private static String resourcesByQueryAndTimeAndActionURL = "resourcesbyqueryandtimestampandaction/";
    private static String resourcesByResultSetIdAndActionURL = "resourcesbyresultsetidandaction/";
    private static String resourcesLogByResultsetAndActionURL = "resourceslogbyresultsetandaction/";
    private static String resourcesLogByResultsetIdAndActionURL = "resourceslogbyresultsetidandaction/";
    private static String commentsByResultsetIdURL = "commentsbyresultsetid/";
    private static String filterQueriesByTimeURL = "filterqueriesbytime/";
    private static String filterSearchHistoryByTimeURL = "filtersearchhistorybytime/";
    private static String recentQueryURL = "getrecentquery/";

    private Client client;
    private int resourceRank;
    private int resultsetId;
    private String sessionId;
    private LinkedList<ViewingTime> viewingTimeList;
    private HashMap<String, LinkedList<Resource>> resourcesList;
    private HashMap<String, Resource> resourcesResultset; //To store all the resources returned for a query or resultSet, only cleared when query changes
    private ArrayList<ResourceLog> resourceLogList; //To keep track of all resource log events till its posted as a batch to the web service
    private LinkedList<Resource> savedResourceList; //To keep track of which resource was saved to Learnweb
    private LinkedList<ResourceLog> resourceClickList;//To keep track of all the resource click events, only cleared when resultSet changes 
    private LinkedList<ResourceLog> resourceSavedList;//To keep track of all the resource saved events, only cleared when resultSet changes 
    private LinkedList<CommentonSearch> searchCommentsList;
    private ArrayList<Tag> tagNamesList;
    private ArrayList<Tag> resultsetTags;

    /**
     * The Default Constructor creates a jersey client and initializes the state variables
     */
    public SearchLogClient(Learnweb learnweb)
    {

        client = Client.create();

        baseURL = learnweb.getProperties().getProperty("SEARCH_TRACKER_URL");
        resourceRank = 0;
        resultsetId = 0;
        viewingTimeList = new LinkedList<ViewingTime>();
        resourcesList = new HashMap<String, LinkedList<Resource>>();
        resourcesResultset = new HashMap<String, Resource>();
        resourceLogList = new ArrayList<ResourceLog>();
        savedResourceList = new LinkedList<Resource>();
        resourceClickList = new LinkedList<ResourceLog>();
        resourceSavedList = new LinkedList<ResourceLog>();
        searchCommentsList = new LinkedList<CommentonSearch>();
        tagNamesList = new ArrayList<Tag>();
        sessionId = "";
    }

    /**
     * This method receives information regarding query, search type, userId, groupId, sessionId, and timestamp from Learnweb2.0 and posts the data to
     * the search log web service
     * 
     * @param query - The query entered by the user during the search process
     * @param searchType - The type of search ( image,text,video )
     * @param userId - The user id of the user currently logged in
     * @param groupId - The group id of the last group accessed by the user
     * @param sessionId - The id of the particular session the user is browsing in
     * @param timestamp - The time stamp when the query was posted
     */
    public void passUserQuery(String query, String searchType, int userId, int groupId, String sessionId, String timestamp)
    {

        QueryLog queryLog = new QueryLog(query, searchType, userId, groupId, sessionId, timestamp);

        WebResource web = client.resource(baseURL + queryLogURL);

        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, queryLog);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("searchlog client SearchLogClient Failed : HTTP error code : " + resp.getStatus(), new Exception());
        }

        ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
        resultsetId = serviceresp.getReturnid();
        flushLists();
    }

    /**
     * This method receives information regarding comment posted by the user for a search process from Learnweb2.0 and posts the data to the search
     * log web service
     * 
     * @param comment
     * @param userId - The user id of the user currently logged in
     * @param timestamp - The time stamp when the comment was posted
     * @param username - The user name of the user currently logged in
     * @param time - The time when the comment was posted
     */
    public void passSearchComment(String comment, int userId, String timestamp, String username, String time)
    {
        int commentId = 0;
        CommentonSearch searchComment = new CommentonSearch(comment, userId, resultsetId, timestamp, username);
        WebResource web = client.resource(baseURL + searchCommentURL);

        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, searchComment);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);

        commentId = serviceresp.getReturnid();
        searchComment.setCommentId(commentId);
        searchComment.setTimestamp(time);
        searchCommentsList.add(searchComment);
    }

    /**
     * This method adds a tag entered by the user to temporary list (resultsetTags or tagNamesList) in the search log client
     * 
     * @param tagName
     * @param userId
     * @param tagList
     */
    public void addToTagList(String tagName, int userId, String tagList)
    {
        Tag tag = new Tag(userId, resultsetId, tagName);
        if(tagList.equals("tagNamesList"))
            tagNamesList.add(tag);
        else if(tagList.equals("resultsetTags"))
        {
            saveTag(tag);
            resultsetTags.add(tag);
        }
    }

    /**
     * This method removes a tag from one of the temporary tag lists
     * 
     * @param tag
     * @param tagList
     */
    public void removeFromTagList(Tag tag, String tagList)
    {
        if(tagList.equals("tagNamesList"))
        {
            if(tagNamesList.contains(tag))
                tagNamesList.remove(tag);
        }
        else if(tagList.equals("resultsetTags"))
        {
            deleteTag(tag.getTagId());
            if(resultsetTags.contains(tag))
                resultsetTags.remove(tag);
        }
    }

    /**
     * This method changes the resultset IDs corresponding to tags present in the temporary lists when the query changes so as to reflect the
     * corresponding resultset ID.
     */
    public void changeTagNamesListResultsetIds()
    {
        for(int i = 0; i < tagNamesList.size(); i++)
        {
            tagNamesList.get(i).setResultsetId(resultsetId);
            tagNamesList.get(i).setTagId(0);
        }
    }

    /**
     * This method is used to post the tag list to the database when there is a query change or towards the end of a session
     */
    public void pushTagList()
    {
        if(tagNamesList.size() > 0)
        {
            TagList tagList = new TagList();
            tagList.setTags(tagNamesList);

            WebResource web = client.resource(baseURL + posttagListURL);
            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, tagList);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }

            tagList = resp.getEntity(TagList.class);
            tagNamesList = tagList.getTags();
        }
    }

    /**
     * This method is used to save a tag directly to the database
     * 
     * @param tag
     * @return
     */
    public Tag saveTag(Tag tag)
    {
        WebResource web = client.resource(baseURL + addtagURL);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, tag);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }
        ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
        tag.setTagId(serviceresp.getReturnid());
        //log.debug(serviceresp.getMessage() + Integer.toString(serviceresp.getReturnid()));

        return tag;
    }

    /**
     * This method is used to delete a tag corresponding to a particular tag ID directly from the database
     * 
     * @param tagId
     */
    public void deleteTag(int tagId)
    {
        WebResource web = client.resource(baseURL + deletetagURL + tagId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).delete(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }
        //ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
        //log.debug(serviceresp.getMessage() + Integer.toString(serviceresp.getReturnid()));
    }

    /**
     * This method gets all the tags entered by a user given the user ID
     * 
     * @param userId
     * @return
     */
    public ArrayList<Tag> getTagsByUserId(int userId)
    {

        WebResource web = client.resource(baseURL + tagsByUserIdURL + userId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        TagList tagList = resp.getEntity(TagList.class);
        return tagList.getTags();
    }

    /**
     * This method gets all the tags corresponding to a search context given the resultset ID
     * 
     * @param resultsetId
     * @return
     */
    public ArrayList<Tag> getTagsByResulsetId(int resultsetId)
    {
        WebResource web = client.resource(baseURL + tagsByResultsetIdURL + resultsetId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        TagList tagList = resp.getEntity(TagList.class);
        return tagList.getTags();
    }

    /**
     * This method allows a user to share a particular resultset with another user by giving the user ID of that user.
     * 
     * @param userId
     * @param userIdToShareWith
     */
    public void postShareResultset(int userId, int userIdToShareWith)
    {
        WebResource web = client.resource(baseURL + shareResultsetURL + userId + "/" + userIdToShareWith + "/" + resultsetId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }
        //ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
        //log.debug(serviceresp.getMessage() + Integer.toString(serviceresp.getReturnid()));
    }

    /**
     * This method returns the list of resultsets shared with a user given the user ID
     * 
     * @param userId
     * @return
     */
    public ArrayList<SharedResultset> getSharedResultsetsByUserId(int userId)
    {

        WebResource web = client.resource(baseURL + sharedResultsetsByUserIdURL + userId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        SharedResultsetList sharedResultsetList = resp.getEntity(SharedResultsetList.class);
        return sharedResultsetList.getSharedResultsets();
    }

    /**
     * This method returns the MD5 value corresponding to a particular resultset ID from the database
     * 
     * @param resultSetIdMd5
     * @return
     */
    public QueryLog getResultsetIdFromMd5Value(String resultSetIdMd5)
    {

        WebResource web = client.resource(baseURL + resultsetIdFromMd5URL + resultSetIdMd5);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        QueryLog resultsetInfo = resp.getEntity(QueryLog.class);

        return resultsetInfo;
    }

    /**
     * This method receives the resources page by page from Learnweb2.0 and posts the resources of the page one by one to the search log web service
     * 
     * @param resources - The resources returned from Learnweb & Interweb
     * @throws IOException
     */
    public void passResultset(LinkedList<ResourceDecorator> resources, MODE mode) throws IOException
    {

        for(ResourceDecorator resource : resources)
        {
            resourceRank++;

            //creating a connection to the search log web service
            WebResource web = client.resource(baseURL + resultsetLogURL);
            Resource resourceSend;
            //creating a new resource type for the web service
            if(mode != MODE.text)
                resourceSend = new Resource(resource.getResource().getId(), resource.getUrl(), resource.getResource().getType().name(), resource.getResource().getSource(), resource.getTitle(), StringHelper.shortnString(resource.getDescription(), 1000),
                        resource.getThumbnail2().getHeight(), resource.getThumbnail2().getWidth(), resource.getThumbnail2().getUrl(), resource.getResource().getThumbnail4().getHeight(), resource.getResource().getThumbnail4().getWidth(),
                        resource.getResource().getThumbnail4().getUrl(), resultsetId, resource.getTempId());
            else
                resourceSend = new Resource(resource.getResource().getId(), resource.getUrl(), resource.getResource().getType().name(), resource.getResource().getSource(), resource.getTitle(), StringHelper.shortnString(resource.getDescription(), 1000), resultsetId,
                        resource.getTempId());

            //posting the resource to the web service
            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, resourceSend);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }
        }

        //log.debug("Successfully stored all the resources");
    }

    /**
     * This method receives the resources as a page from Learnweb2.0 and posts the entire batch of results to the search log web service
     * 
     * @param resources The resources returned from Learnweb & Interweb
     */
    public void passBatchResultset(LinkedList<ResourceDecorator> resources, MODE mode)
    {

        LinkedList<Resource> resourcelist = new LinkedList<Resource>();

        for(ResourceDecorator resource : resources)
        {
            resourceRank++;
            Resource resourceSend;
            //creating a new resource type for the web service
            if(mode != MODE.text)
                resourceSend = new Resource(resource.getResource().getId(), resource.getUrl(), resource.getResource().getType().name(), resource.getResource().getSource(), resource.getTitle(), StringHelper.shortnString(resource.getDescription(), 1000),
                        resource.getThumbnail2().getHeight(), resource.getThumbnail2().getWidth(), resource.getThumbnail2().getUrl(), resource.getResource().getThumbnail4().getHeight(), resource.getResource().getThumbnail4().getWidth(),
                        resource.getResource().getThumbnail4().getUrl(), resultsetId, resource.getTempId());
            else
                resourceSend = new Resource(resource.getResource().getId(), resource.getUrl(), resource.getResource().getType().name(), resource.getResource().getSource(), resource.getTitle(), StringHelper.shortnString(resource.getDescription(), 1000), resultsetId,
                        resource.getTempId());
            resourcelist.add(resourceSend);
        }

        ResourceList rs_list = new ResourceList();
        rs_list.setResources(resourcelist);

        //creating a connection to the search log web service
        WebResource web = client.resource(baseURL + batchResultsetLogURL);
        //posting the resource to the web service
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, rs_list);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        //log.debug(resp.getEntity(ServiceResponse.class).getMessage() + Integer.toString(resp.getStatus()));
    }

    /**
     * This method receives the page no, mode (image,web,video) and resources page by page and they are saved in a HashMap data structure in the
     * client to be posted to the
     * web service at a later time.
     * 
     * @param page - The page no.
     * @param mode - search type (image,text,video)
     * @param serp - The list of results displayed on the search results page
     */
    public void saveSERP(int page, MODE mode, LinkedList<ResourceDecorator> serp)
    {
        LinkedList<Resource> resources = new LinkedList<Resource>();
        String pageKey = Integer.toString(mode.ordinal()) + Integer.toString(page);

        if(!resourcesList.containsKey(pageKey) && serp.size() > 0)
        {
            for(ResourceDecorator resource : serp)
            {
                resourceRank++;
                //creating a new resource type for the web service
                Resource resourceSend;
                if(mode != MODE.text)
                    resourceSend = new Resource(resource.getResource().getId(), resource.getUrl(), resource.getResource().getType().name(), resource.getResource().getSource(), resource.getTitle(), StringHelper.shortnString(resource.getDescription(), 1000),
                            resource.getThumbnail2().getHeight(), resource.getThumbnail2().getWidth(), resource.getThumbnail2().getUrl(), resource.getResource().getThumbnail4().getHeight(), resource.getResource().getThumbnail4().getWidth(),
                            resource.getResource().getThumbnail4().getUrl(), resultsetId, resource.getTempId());
                else
                    resourceSend = new Resource(resource.getResource().getId(), resource.getUrl(), resource.getResource().getType().name(), resource.getResource().getSource(), resource.getTitle(), StringHelper.shortnString(resource.getDescription(), 1000), resultsetId,
                            resource.getTempId());

                resources.add(resourceSend);
                resourcesResultset.put(resource.getUrl(), resourceSend);
            }
            resourcesList.put(pageKey, resources);
        }
    }

    /**
     * This method posts the batch of resources stored in the HashMap to the search log web service after a particular timeout or session end/change
     */
    public void pushBatchResultsetList()
    {
        LinkedList<Resource> resourcesBatch = new LinkedList<Resource>();

        if(resourcesList.size() > 0)
        {
            Iterator<String> iter = resourcesList.keySet().iterator();

            while(iter.hasNext())
            {
                String pageKey = iter.next();
                resourcesBatch.addAll(resourcesList.get(pageKey));
            }

            ResourceList rsList = new ResourceList();
            rsList.setResources(resourcesBatch);

            resourcesList.clear();

            //creating a connection to the search log web service
            WebResource web = client.resource(baseURL + batchResultsetLogURL);
            //posting the resource to the web service
            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, rsList);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }

            //log.debug(resp.getEntity(ServiceResponse.class).getMessage() + Integer.toString(resp.getStatus()));
        }
    }

    /**
     * This method receives the a particular action from the user on a resource and saves it to a resource log list to post it later to the service
     * 
     * @param userId - The id of the user
     * @param date - The time stamp when a particular action was recorded on the resource
     * @param action - The type of action whether resource_click, resource_saved, resource_dialog_open
     * @param url - The URL of the resource on which the action was issued
     * @param resourceRank - The rank of the resource in the current result set
     * @param filename - The filename of the resource
     * @param source - The original source of the resource
     */
    public void saveResourceLog(int userId, Date date, ACTION action, String url, int resourceRank, String filename, String source)
    {

        ResourceLog resourceLog = new ResourceLog(userId, resultsetId, resourceRank, action.name(), new Timestamp(date.getTime()).toString(), url);
        resourceLogList.add(resourceLog);

        SimpleDateFormat actionDateToTime = new SimpleDateFormat("HH:mm:ss");
        String action_time = actionDateToTime.format(date);
        resourceLog = new ResourceLog(userId, resultsetId, resourceRank, action.name(), action_time, url, filename, source);

        if(action == ACTION.resource_click)
        {
            resourceClickList.add(resourceLog);
        }
        else if(action == ACTION.resource_saved)
        {
            resourceSavedList.add(resourceLog);
        }
    }

    /**
     * This method posts the batch of resource log events saved in the resourceLog list to the database.
     */
    public void postResourceLog()
    {

        if(resourceLogList.size() > 0)
        {
            ResourceLogList batchResourceLog = new ResourceLogList();
            batchResourceLog.setResourceLog(resourceLogList);

            WebResource web = client.resource(baseURL + resourceLogURL);
            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, batchResourceLog);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }

            resourceLogList.clear();
            //ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
            //log.debug(serviceresp.getMessage() + "successfully captured : " + serviceresp.getReturnid());
        }
    }

    /**
     * This method adds a resource saved event to a resource saved list to post it to the service later
     *
     * @param resourceRank
     * @param systemId
     */
    public void addResourceSavedList(int resourceRank, int systemId)
    {
        Resource resource_saved = new Resource(resourceRank, resultsetId, systemId);
        savedResourceList.add(resource_saved);
    }

    /**
     * This method is called after the result set is updated and it updates the resources table to show that a particular resource was selected and
     * saved by the user
     */
    public void passUpdateResultset()
    {

        if(savedResourceList.size() > 0)
        {
            ResourceList tempSavedResourceList = new ResourceList();
            tempSavedResourceList.setResources(savedResourceList);

            WebResource web = client.resource(baseURL + updateResultsetURL);

            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, tempSavedResourceList);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }

            savedResourceList.clear();
            //ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
            //log.debug("Successfully updated the resultset table: " + serviceresp.getReturnid());
        }

    }

    /**
     * This method posts the viewing time of a resource one by one to the search log web service
     * 
     * @param resourceRank - The rank of the resource being viewed
     * @param startTime - The start time stamp when the user started viewing the resource
     * @param endTime - The end time stamp when the user finished viewing the resource
     */
    public void passViewingTime(int resourceRank, Date startTime, Date endTime)
    {

        ViewingTime viewingTime = new ViewingTime(resultsetId, resourceRank, startTime, endTime);
        viewingTimeList.add(viewingTime);

        WebResource web = client.resource(baseURL + viewingTimeURL);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, viewingTime);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        //ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
        //log.debug("Successfully updated the viewing time table: " + serviceresp.getReturnid());

    }

    /**
     * This method receives the viewing times of resources from Learnweb2.0 and stores it in linkedlist and posts the viewing time of resources as a
     * batch to the
     * search log web service after a session change or stack limit
     * 
     * @param resourceRank - The temporary rank or id assigned to a resource being displayed on the search results page
     * @param startTime - The start timestamp when the user started viewing the resource
     * @param endTime - The end timestamp when the user finished viewing the resource
     * @param sessionId - The id of that particular session
     */
    public void passBatchViewingTime(int resourceRank, Date startTime, Date endTime, String sessionId)
    {

        ViewingTime viewingTime = new ViewingTime(resultsetId, resourceRank, startTime, endTime);
        viewingTimeList.add(viewingTime);

        if((!this.sessionId.equals(sessionId)) || (viewingTimeList.size() > 100))
        {
            setSessionId(sessionId);
            ViewingTimeList vt_list = new ViewingTimeList();
            vt_list.setViewingTimeList(viewingTimeList);

            WebResource web = client.resource(baseURL + batchViewingTimeURL);

            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).post(ClientResponse.class, vt_list);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }

            viewingTimeList.clear();
            //ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
            //log.debug("Successfully updated the viewing time table: " + serviceresp.getReturnid());
        }

    }

    /**
     * This method is called from the client to truncate all tables in the database
     */
    public void truncateTables()
    {
        WebResource web = client.resource(baseURL + truncateTablesURL);

        ClientResponse resp = web.delete(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        //ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
        //log.debug(serviceresp.getMessage() + serviceresp.getReturnid());
    }

    /**
     * This method receives a userId from Learnweb2.0 and deletes all the entries corresponding to userId in the database by calling the method in the
     * web service
     * 
     * @param userId - The id of the user to be removed
     */
    public void removeUserQueries(int userId)
    {

        WebResource web = client.resource(baseURL + removeUserURL + userId);

        ClientResponse resp = web.delete(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        //ServiceResponse serviceresp = resp.getEntity(ServiceResponse.class);
        //log.debug(serviceresp.getMessage() + serviceresp.getReturnid());
    }

    /**
     * This method calls the POST request instead of the DELETE request as needed by the rest endpoint because in java version 1.7, there is a bug in
     * HttpURLConnection which
     * prevents writing an entity onto the request body of the DELETE request.
     * 
     * @param resultsetIds
     */
    public void deleteUserQueries(ArrayList<Integer> resultsetIds)
    {

        if(resultsetIds.size() > 0)
        {
            ResultSetIdList resultSetIdList = new ResultSetIdList();
            resultSetIdList.getResultsetId().addAll(resultsetIds);

            //creating a connection to the search log web service
            WebResource web = client.resource(baseURL + deleteUserQueriesURL);

            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).header("X-HTTP-Method-Override", "DELETE").post(ClientResponse.class, resultSetIdList);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }

            //log.debug(resp.getEntity(ServiceResponse.class).getMessage() + Integer.toString(resp.getStatus()));
        }
    }

    /**
     * This method returns the search history grouped by date
     * 
     * @param userId - The id of the user whose search history has to be retrieved
     * @return - It returns an array list of the search history grouped by date
     */
    public ArrayList<HistoryByDate> getSearchHistoryByDate(int userId)
    {

        ArrayList<HistoryByDate> historyByDates = new ArrayList<HistoryByDate>();

        if(userId != -1)
        {
            WebResource web = client.resource(baseURL + searchHistoryByDateURL + userId);

            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }

            HistoryByDateList historyByDateList = resp.getEntity(HistoryByDateList.class);
            historyByDates.addAll(historyByDateList.getHistoryByDates());
        }

        return historyByDates;
    }

    /**
     * This method is used to retrieve the search history page by page by setting an offset and limit which is the page size.
     * 
     * @param userId - search history of which user
     * @param offset - the number from where the search history should start retrieving
     * @param limit - the number of history results that should be retrieved per page
     * @return - Returns an arraylist of search history results retrieved grouped by date
     */
    public ArrayList<HistoryByDate> getSearchHistoryByPages(int userId, int offset, int limit)
    {

        ArrayList<HistoryByDate> historyByDates = new ArrayList<HistoryByDate>();

        if(userId != -1)
        {
            WebResource web = client.resource(baseURL + searchHistoryByPagesURL + userId + "/" + offset + "/" + limit);

            ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

            if(resp.getStatus() != 200)
            {
                throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
            }

            HistoryByDateList historyByDateList = resp.getEntity(HistoryByDateList.class);
            historyByDates.addAll(historyByDateList.getHistoryByDates());
        }

        return historyByDates;
    }

    /**
     * This method returns the query history grouped by date
     * 
     * @param userId - The user id whose query history has to be retrieved
     * @param query - The query entered by the user
     * @return - It returns an array list of query history grouped by date
     */
    public ArrayList<HistoryByDate> getQueryHistory(int userId, String query)
    {

        try
        {
            query = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20");
        }
        catch(UnsupportedEncodingException e)
        {
            log.error("unhandled error", e);
        }

        ArrayList<HistoryByDate> queryHistoryByDate = new ArrayList<HistoryByDate>();

        WebResource web = client.resource(baseURL + searchHistoryByQueryURL + userId + "/" + query);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        HistoryByDateList historyByDateList = resp.getEntity(HistoryByDateList.class);
        queryHistoryByDate.addAll(historyByDateList.getHistoryByDates());

        return queryHistoryByDate;
    }

    /**
     * This method returns the query history grouped by date for a given date range
     * 
     * @param startTimestamp
     * @param endTimestamp
     * @return It returns a filtered array list of query history grouped by date
     */
    public ArrayList<HistoryByDate> filterQueryHistoryByDates(String startTimestamp, String endTimestamp)
    {

        try
        {
            startTimestamp = java.net.URLEncoder.encode(startTimestamp, "UTF-8").replace("+", "%20");
            endTimestamp = java.net.URLEncoder.encode(endTimestamp, "UTF-8").replace("+", "%20");
        }
        catch(UnsupportedEncodingException e)
        {
            log.error("unhandled error", e);
        }

        ArrayList<HistoryByDate> queryHistoryByDate = new ArrayList<HistoryByDate>();

        WebResource web = client.resource(baseURL + filterQueriesByTimeURL + startTimestamp + "/" + endTimestamp);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        HistoryByDateList historyByDateList = resp.getEntity(HistoryByDateList.class);
        queryHistoryByDate.addAll(historyByDateList.getHistoryByDates());

        return queryHistoryByDate;
    }

    /**
     * This method filters the search history by dates between two given timestamps.
     * 
     * @param userId - The search history for which user
     * @param startTimestamp - The starting timestamp from where to retrieve the search history
     * @param endTimestamp - The ending timestamp until where the search history needs to be retrieved
     * @return - Returns an arraylist of search history results retrieved grouped by date
     */
    public ArrayList<HistoryByDate> filterSearchHistoryByDates(int userId, String startTimestamp, String endTimestamp)
    {

        try
        {
            startTimestamp = java.net.URLEncoder.encode(startTimestamp, "UTF-8").replace("+", "%20");
            endTimestamp = java.net.URLEncoder.encode(endTimestamp, "UTF-8").replace("+", "%20");
        }
        catch(UnsupportedEncodingException e)
        {
            log.error("unhandled error", e);
        }

        ArrayList<HistoryByDate> searchHistoryByDates = new ArrayList<HistoryByDate>();

        WebResource web = client.resource(baseURL + filterSearchHistoryByTimeURL + userId + "/" + startTimestamp + "/" + endTimestamp);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        HistoryByDateList historyByDateList = resp.getEntity(HistoryByDateList.class);
        searchHistoryByDates.addAll(historyByDateList.getHistoryByDates());

        return searchHistoryByDates;
    }

    /**
     * This method returns an URL list of resources for a given query
     * 
     * @param query
     * @return - It returns an array list of the URL's of resources selected for the given query
     */
    public ArrayList<String> getResourcesByQuery(String query)
    {
        try
        {
            query = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20");
        }
        catch(UnsupportedEncodingException e)
        {
            log.error("unhandled error", e);
        }

        ArrayList<String> resourcesUrlList = new ArrayList<String>();

        WebResource web = client.resource(baseURL + resourcesByQueryURL + query);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        UrlList urlList = resp.getEntity(UrlList.class);
        resourcesUrlList.addAll(urlList.getResources_urls());

        return resourcesUrlList;
    }

    /**
     * This method returns an URL list of resources for a given resultsetid
     * 
     * @param resultsetId
     * @return - It returns an array list of the URL's of resources selected for the given query and time stamp
     */
    public ArrayList<String> getResourceUrlsByResultsetId(int resultsetId)
    {

        ArrayList<String> resourcesUrlList = new ArrayList<String>();

        WebResource web = client.resource(baseURL + resourceUrlsByResultsetIdURL + resultsetId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        UrlList urlList = resp.getEntity(UrlList.class);
        resourcesUrlList.addAll(urlList.getResources_urls());

        return resourcesUrlList;
    }

    /**
     * This method returns a set of resources for a given query and time stamp in a form that can fit into the learnWeb interface
     * 
     * @param query - The query for which resources needs to be returned
     * @param timestamp - The query time stamp for which resources needs to be returned
     * @return - It returns a linked list of the resources for a given query and time stamp
     */
    public LinkedList<ResourceDecorator> getResourcesByQueryAndTimestamp(String query, String timestamp)
    {
        try
        {
            query = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20");
            timestamp = java.net.URLEncoder.encode(timestamp, "UTF-8").replace("+", "%20");

        }
        catch(UnsupportedEncodingException e)
        {
            log.error("unhandled error", e);
        }

        WebResource web = client.resource(baseURL + resourcesByQueryAndTimestampURL + query + "/" + timestamp);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        LinkedList<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();
        ResourceList resourcesList = resp.getEntity(ResourceList.class);
        if(resourcesList.getResources() != null)
        {
            for(Resource resource : resourcesList.getResources())
            {
                de.l3s.learnweb.Resource tempResource = new de.l3s.learnweb.Resource(resource.getResourceId(), resource.getShortdescrp(), resource.getFilename(), resource.getSource(), resource.getThumbnail_height(), resource.getThumbnail_width(), resource.getThumbnail_url(),
                        resource.getThumbnail4_height(), resource.getThumbnail4_width(), resource.getThumbnail4_url(), resource.getUrl(), resource.getType());
                ResourceDecorator decoratedResource = new ResourceDecorator(tempResource);
                decoratedResource.setTempId(resource.getResource_rank());
                resources.add(decoratedResource);
            }
        }
        return resources;
    }

    /**
     * Returns the resources from the database for that particular resultset ID
     * 
     * @param resultSetId - The resultset ID from which the resources should be retrieved
     * @return - LinkedList of ResourceDecorator objects which contains all the resources returned from the database
     */
    public LinkedList<ResourceDecorator> getResourcesByResultSetId(int resultSetId)
    {

        WebResource web = client.resource(baseURL + resourcesByResultSetIdURL + resultSetId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new IllegalStateException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        LinkedList<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();
        ResourceList resourcesList = resp.getEntity(ResourceList.class);
        if(resourcesList.getResources() != null)
        {
            for(Resource resource : resourcesList.getResources())
            {
                resourcesResultset.put(resource.getUrl(), resource);
                de.l3s.learnweb.Resource tempResource = new de.l3s.learnweb.Resource(resource.getResourceId(), resource.getShortdescrp(), resource.getFilename(), resource.getSource(), resource.getThumbnail_height(), resource.getThumbnail_width(), resource.getThumbnail_url(),
                        resource.getThumbnail4_height(), resource.getThumbnail4_width(), resource.getThumbnail4_url(), resource.getUrl(), resource.getType());
                tempResource.setLocation(tempResource.getSource());
                ResourceDecorator decoratedResource = new ResourceDecorator(tempResource);
                decoratedResource.setTempId(resource.getResource_rank());
                resources.add(decoratedResource);
            }
        }
        return resources;
    }

    /**
     * This method returns a set of resources for a given query, time stamp and action in a form that can fit into the learnWeb interface
     * 
     * @param query - The query for which resources needs to be returned
     * @param timestamp - The query time stamp for which resources needs to be returned
     * @param action - The action of the resource events
     * @return - It returns a linked list of the resources for a given query and time stamp as well as action
     */
    public LinkedList<ResourceDecorator> getResourcesByQueryAndTimestampAndAction(String query, String timestamp, String action)
    {
        try
        {
            query = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20");
            timestamp = java.net.URLEncoder.encode(timestamp, "UTF-8").replace("+", "%20");

        }
        catch(UnsupportedEncodingException e)
        {
            log.error("unhandled error", e);
        }

        WebResource web = client.resource(baseURL + resourcesByQueryAndTimeAndActionURL + query + "/" + timestamp + "/" + action);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        LinkedList<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();
        ResourceList resourcesList = resp.getEntity(ResourceList.class);
        if(resourcesList.getResources() != null)
        {
            for(Resource resource : resourcesList.getResources())
            {
                de.l3s.learnweb.Resource tempResource = new de.l3s.learnweb.Resource(resource.getResourceId(), resource.getShortdescrp(), resource.getFilename(), resource.getSource(), resource.getThumbnail_height(), resource.getThumbnail_width(), resource.getThumbnail_url(),
                        resource.getThumbnail4_height(), resource.getThumbnail4_width(), resource.getThumbnail4_url(), resource.getUrl(), resource.getType());
                ResourceDecorator decoratedResource = new ResourceDecorator(tempResource);
                decoratedResource.setTempId(resource.getResource_rank());
                resources.add(decoratedResource);
            }
        }
        return resources;
    }

    /**
     * This method gets the resources for a given resultset ID and action which could be resource_click or resource_saved
     * 
     * @param resultSetId - resultset ID of the resources needed
     * @param action - Which action to filter the resultset by resource_click or resource_saved
     * @return - LinkedList of ResourceDecorator objects containing the filtered resources for that resultset given an action
     */
    public LinkedList<ResourceDecorator> getResourcesByResultSetIdAndAction(int resultSetId, String action)
    {

        WebResource web = client.resource(baseURL + resourcesByResultSetIdAndActionURL + resultSetId + "/" + action);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        LinkedList<ResourceDecorator> resources = new LinkedList<ResourceDecorator>();
        ResourceList resourcesList = resp.getEntity(ResourceList.class);
        if(resourcesList.getResources() != null)
        {
            for(Resource resource : resourcesList.getResources())
            {
                de.l3s.learnweb.Resource tempResource = new de.l3s.learnweb.Resource(resource.getResourceId(), resource.getShortdescrp(), resource.getFilename(), resource.getSource(), resource.getThumbnail_height(), resource.getThumbnail_width(), resource.getThumbnail_url(),
                        resource.getThumbnail4_height(), resource.getThumbnail4_width(), resource.getThumbnail4_url(), resource.getUrl(), resource.getType());
                ResourceDecorator decoratedResource = new ResourceDecorator(tempResource);
                decoratedResource.setTempId(resource.getResource_rank());
                resources.add(decoratedResource);
            }
        }
        return resources;
    }

    /**
     * This method retrieves the resource log for a given resultset ID and particular action
     * 
     * @param query - The query and timestamp are used to retrieve the unique resultset ID corresponding to it
     * @param timestamp
     * @param action - The action corresponds to the filter that needs to be applied on the resource log that needs to be returned.
     * @return - It returns an arrylist of filtered resource log for a given resultset ID by action.
     */
    public ArrayList<ResourceLog> getResourcesLogByResultsetAndAction(String query, String timestamp, String action)
    {
        try
        {
            query = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20");
            timestamp = java.net.URLEncoder.encode(timestamp, "UTF-8").replace("+", "%20");

        }
        catch(UnsupportedEncodingException e)
        {
            log.error("unhandled error", e);
        }

        WebResource web = client.resource(baseURL + resourcesLogByResultsetAndActionURL + query + "/" + timestamp + "/" + action);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        ResourceLogList resourcesLog = resp.getEntity(ResourceLogList.class);

        return resourcesLog.getResourceLog();
    }

    /**
     * This method retrieves the resource log for a given resultset ID and particular action
     * 
     * @param resultSetId - This is used to determine which resultset
     * @param action - The action by which to filter the resourcelog retrieved for that resultset
     * @return - ArrayList of filtered resource logs.
     */
    public ArrayList<ResourceLog> getResourcesLogByResultsetIdAndAction(int resultSetId, String action)
    {

        WebResource web = client.resource(baseURL + resourcesLogByResultsetIdAndActionURL + resultSetId + "/" + action);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        ResourceLogList resourcesLog = resp.getEntity(ResourceLogList.class);

        return resourcesLog.getResourceLog();
    }

    /**
     * This method returns all the comments on a particular search process given a resultset ID.
     * 
     * @param resultSetId
     * @return - An arraylist of search comments.
     */
    public ArrayList<CommentonSearch> getSearchCommentsByResultsetId(int resultSetId)
    {

        WebResource web = client.resource(baseURL + commentsByResultsetIdURL + resultSetId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        SearchCommentsList searchComments = resp.getEntity(SearchCommentsList.class);

        return searchComments.getComments();
    }

    /**
     * This method returns the most recent query posted by the user.
     * 
     * @return
     */
    public QueryLog getRecentQuery(int userId)
    {

        WebResource web = client.resource(baseURL + recentQueryURL + userId);
        ClientResponse resp = web.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

        if(resp.getStatus() != 200)
        {
            throw new RuntimeException("SearchLogClient Failed : HTTP error code : " + resp.getStatus());
        }

        QueryLog recentQuery = resp.getEntity(QueryLog.class);
        return recentQuery;
    }

    /**
     * It flushes all the arraylists which helps in keeping track of the current search.
     */
    public void flushLists()
    {
        resourceRank = 0;
        resourcesResultset.clear();
        resourceClickList.clear();
        resourceSavedList.clear();
        searchCommentsList.clear();
        viewingTimeList.clear();
    }

    /**
     * Returns the resource rank or temporary resource id corresponding to that resource in the current resultset using the given url.
     * 
     * @param Url
     * @return - It returns the resource rank.
     */
    public int getResourceIdByUrl(String Url)
    {
        return resourcesResultset.get(Url).getResource_rank();
    }

    public int getResourceRank()
    {
        return resourceRank;
    }

    public void setResourceRank(int resourceRank)
    {
        this.resourceRank = resourceRank;
    }

    public int getResultsetid()
    {
        return resultsetId;
    }

    public void setResultsetid(int resultsetid)
    {
        this.resultsetId = resultsetid;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public LinkedList<ResourceLog> getResourceClickList()
    {
        return resourceClickList;
    }

    public LinkedList<ResourceLog> getResourceSavedList()
    {
        return resourceSavedList;
    }

    public LinkedList<ViewingTime> getViewingTimeList()
    {
        return viewingTimeList;
    }

    public LinkedList<CommentonSearch> getSearchCommentsList()
    {
        return searchCommentsList;
    }

    public ArrayList<Tag> getTagNamesList()
    {
        return tagNamesList;
    }

    public ArrayList<Tag> getResultsetTags()
    {
        if(resultsetTags == null)
        {
            resultsetTags = new ArrayList<Tag>();
            resultsetTags.addAll(getTagsByResulsetId(resultsetId));
        }
        return resultsetTags;
    }

    public void setResultsetTags(ArrayList<Tag> resultsetTags)
    {
        this.resultsetTags = resultsetTags;
    }
}
