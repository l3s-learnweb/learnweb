package de.l3s.archiveSearch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.util.Image;
import de.l3s.util.StringHelper;

public class ArchiveItShingle
{

    private final int w = 25; // the N-gram dimension i.e w words in a shingle

    public Set<String> computeShingles(List<String> wordList)
    {
	Set<String> setOfShingles = new HashSet<String>();
	List<String> shingleList = new LinkedList<String>();
	for(int i = 0; i < wordList.size() - w; i++)
	{
	    shingleList = wordList.subList(i, i + w);
	    setOfShingles.add(StringUtils.join(shingleList, " ").toLowerCase());
	}
	return setOfShingles;
    }

    public float computeJaccardIndex(Set<String> set1, Set<String> set2)
    {
	if(set1 == null || set2 == null)
	    return 0;

	Set<String> intersect = new HashSet<String>(set1);
	intersect.retainAll(set2);
	Set<String> union = new HashSet<String>(set1);
	union.addAll(set2);
	if(union.size() == 0)
	    return 0;
	return (float) intersect.size() / union.size();
    }

    public void processWebsite(String url, String timestamp) throws IOException
    {
	URL thumbnailUrl = new URL("http://prometheus.kbs.uni-hannover.de/thumbnail/thumb_wb.php?url=" + StringHelper.urlEncode(url));
	Image img = new Image(thumbnailUrl.openStream());
	java.io.File actualFile = new java.io.File("F://DevTools//Work stuff//Crawler//Thumbnails//", timestamp + ".png");
	OutputStream outputStream = new FileOutputStream(actualFile);
	IOUtils.copy(img.getInputStream(), outputStream);
	outputStream.close();
    }

    public void processThumbnails(Set<String> set, String type) throws IOException
    {
	for(String str : set)
	    processWebsite(str, type + "-" + str.substring(34, 45));
    }

    /**
     * traverse HTML DOM structure
     * 
     */
    private NodeVisitor processNode(final StringBuilder htmlString)
    {
	NodeVisitor node = new NodeVisitor()
	{
	    @Override
	    public void head(Node node, int depth)
	    {
		if(node instanceof Element)
		    htmlString.append(node.nodeName() + " ");
	    }

	    @Override
	    public void tail(Node node, int depth)
	    {
		if(node instanceof Element)
		    htmlString.append(node.nodeName() + " ");
	    }
	};
	return node;
    }

    /**
     * return unique archives by comparing all pairs
     */
    public Set<String> computeUniqueArchivesByPair(HashMap<String, Set<String>> archiveUrls)
    {
	float d = 0;
	Set<String> nearDuplicateUrls = new HashSet<String>();
	Set<String> uniqueUrls = new HashSet<String>();
	for(Map.Entry<String, Set<String>> entry1 : archiveUrls.entrySet())
	{
	    for(Map.Entry<String, Set<String>> entry2 : archiveUrls.entrySet())
	    {
		if(entry1 != entry2 && !nearDuplicateUrls.contains(entry2.getKey()))
		{
		    if(!uniqueUrls.contains(entry2.getKey()))
		    {
			d = computeJaccardIndex(entry1.getValue(), entry2.getValue());
			if(d <= 0.5)
			{
			    uniqueUrls.add(entry1.getKey());
			    uniqueUrls.add(entry2.getKey());
			}
		    }
		}
	    }
	    nearDuplicateUrls.add(entry1.getKey());
	}
	return uniqueUrls;
    }

    /**
     * Updates duplicate shingle ids in lw_resource_archiveurl based on html
     * text and tags
     */
    public void getDuplicateShingles(int resourceId, int size) throws SQLException
    {
	List<Integer> duplicateShingleId = new ArrayList<Integer>();
	Connection conn = Learnweb.getInstance().getConnection();
	PreparedStatement ps = conn.prepareStatement("SELECT `shingle_id` FROM `lw_resource_archive_shingles` natural join `lw_resource_archiveurl` where `resource_id`=? group by `htmltext`, `htmltags` ORDER BY `lw_resource_archive_shingles`.`shingle_id` ASC");
	ps.setInt(1, resourceId);
	ResultSet rs = ps.executeQuery();
	while(rs.next())
	    duplicateShingleId.add(rs.getInt("shingle_id"));
	duplicateShingleId.add(duplicateShingleId.get(0) + size - 1);
	size = duplicateShingleId.get(duplicateShingleId.size() - 1);
	int j = 0;
	for(int i = duplicateShingleId.get(0); i <= size; i++)
	{
	    if(duplicateShingleId.contains(i))
	    {
		j++;
	    }
	    else
	    {
		ps = conn.prepareStatement("UPDATE `lw_resource_archiveurl` SET `shingle_id`=? where `resource_id`=? and `shingle_id`=?");
		ps.setInt(1, duplicateShingleId.get(j - 1));
		ps.setInt(2, resourceId);
		ps.setInt(3, i);
		ps.execute();
	    }
	}
	ps.close();
    }

    public void generateThumbnails(int resourceId) throws IOException, SQLException
    {
	ResourcePreviewMaker resourcePreviewMaker = Learnweb.getInstance().getResourcePreviewMaker();
	Connection conn = Learnweb.getInstance().getConnection();
	PreparedStatement ps = conn.prepareStatement("SELECT `archive_url`,`httpstatuscode` FROM `lw_resource_archiveurl` where `resource_id`=? group by `shingle_id`");
	ps.setInt(1, resourceId);
	ResultSet rs = ps.executeQuery();
	while(rs.next())
	{
	    if(rs.getInt("httpstatuscode") == 200)
		resourcePreviewMaker.processArchiveWebsite(resourceId, rs.getString("archive_url"));
	}
	ps.close();
    }

    public void computeUniqueArchivesByPair(HashMap<String, String> archiveUrls, int resourceId) throws SQLException
    {
	Set<String> setOfNearUniqueArchivesPair = new HashSet<String>();
	ArrayList<String> values = new ArrayList<String>(archiveUrls.values());
	int d = values.size();
	int t = 0;
	for(int i = 0; i < d - 1; i++)
	{
	    if(values.get(i).equals(values.get(i + 1)))
	    {
		t++;
		setOfNearUniqueArchivesPair.add(values.get(i));
	    }
	}
	System.out.println(resourceId + " Amount of duplicates:" + t + " Total archive versions:" + d + " Percentage dublicates:" + (float) t * 100 / d);
    }

    /**
     * Detecting near duplicates based on sequence algorithm
     * 
     */
    public Set<String> computeUniqueArchivesBySequence(HashMap<String, Set<String>> hashmaptext, HashMap<String, Set<String>> hashmapframe, List<ArchiveUrl> archiveUrls, int resourceId, float frameThreshold, float textThreshold) throws SQLException
    {
	Set<String> uniqueUrls = new LinkedHashSet<String>();
	Connection conn = Learnweb.getInstance().getConnection();
	int j = 0;
	float textSim = 0, frameSim = 0;
	String url = null;
	String key = archiveUrls.get(j).getArchiveUrl();

	for(int i = 1; i < archiveUrls.size(); i++)
	{

	    url = archiveUrls.get(i).getArchiveUrl();
	    Timestamp timestamp1 = new Timestamp(archiveUrls.get(j).getTimestamp().getTime());
	    Timestamp timestamp2 = new Timestamp(archiveUrls.get(i).getTimestamp().getTime());
	    PreparedStatement ps = conn.prepareStatement("SELECT `jaccard_text` ,`jaccard_frame` FROM `lw_resource_archive_jaccardindex` WHERE `resource_id`=? AND `timestamp1`=? AND `timestamp2`=?");
	    ps.setInt(1, resourceId);
	    ps.setTimestamp(2, timestamp1);
	    ps.setTimestamp(3, timestamp2);
	    ResultSet rs = ps.executeQuery();
	    if(rs.next())
	    {
		textSim = rs.getFloat("jaccard_text");
		frameSim = rs.getFloat("jaccard_frame");
	    }
	    else
	    {
		textSim = computeJaccardIndex(hashmaptext.get(url), hashmaptext.get(key));
		frameSim = computeJaccardIndex(hashmapframe.get(url), hashmapframe.get(key));
		ps = conn.prepareStatement("REPLACE INTO `lw_resource_archive_jaccardindex` VALUES(?,?,?,?,?)");
		ps.setInt(1, resourceId);
		ps.setTimestamp(2, timestamp1);
		ps.setTimestamp(3, timestamp2);
		ps.setFloat(4, frameSim);
		ps.setFloat(5, textSim);
		ps.execute();
	    }

	    if(Float.compare(textSim, textThreshold) <= 0 && Float.compare(frameSim, frameThreshold) <= 0)
	    {
		uniqueUrls.add(key);
		key = url;
		j = i;
	    }
	}
	uniqueUrls.add(key);
	return uniqueUrls;
    }

    public static void main(String[] args) throws IOException, SQLException
    {
	String url = null;
	final StringBuilder htmlString = new StringBuilder();

	ArchiveItShingle archiveItShingle = new ArchiveItShingle();

	Group group = Learnweb.getInstance().getGroupManager().getGroupById(1132);
	List<Resource> listOfResources = new LinkedList<Resource>(group.getResources());

	Connection conn = Learnweb.getInstance().getConnection();
	for(int j = 37; j < 38 && j != 44; j++)
	{

	    int resource_id = listOfResources.get(j).getId();
	    List<ArchiveUrl> listOfArchives = new LinkedList<ArchiveUrl>();

	    listOfArchives = listOfResources.get(j).getArchiveUrls();
	    List<String> wordList = new ArrayList<String>();

	    //archiveItShingle.getDublicateShingles(resource_id, listOfArchives.size());
	    //archiveItShingle.generateThumbnails(resource_id);

	    Set<String> setOfShingles = new HashSet<String>();
	    Set<String> setOfNearUniqueArchives = new HashSet<String>();

	    HashMap<String, Set<String>> hashmapframe = new LinkedHashMap<String, Set<String>>();
	    HashMap<String, Set<String>> hashmaptext = new LinkedHashMap<String, Set<String>>();
	    HashMap<String, String> hashmapstring = new LinkedHashMap<String, String>();
	    hashmapframe.clear();
	    hashmaptext.clear();
	    try
	    {
		for(ArchiveUrl archiveUrl : listOfArchives)
		{
		    wordList.clear();
		    setOfShingles.clear();
		    url = archiveUrl.getArchiveUrl();
		    Document document = null;
		    org.jsoup.Connection urlConnection = Jsoup.connect(url).ignoreHttpErrors(true).timeout(900000);
		    Response response = urlConnection.execute();
		    if(response.statusCode() == 200)
			document = urlConnection.get();
		    else
		    {
			PreparedStatement ps = conn.prepareStatement("UPDATE `lw_resource_archiveurl` SET `httpstatuscode`=? WHERE `resource_id`=? AND `archive_url`=?");
			ps.setInt(1, response.statusCode());
			ps.setInt(2, resource_id);
			ps.setString(3, url);
			ps.execute();
			ps.close();
			continue;
		    }

		    document.select("wb_div#wm-disclaim, script, style, head").remove(); //remove Archive disclaimer from html text
		    document.traverse(archiveItShingle.processNode(htmlString));
		    String[] words = htmlString.toString().replaceAll("[!?,.]", "").split(" ");
		    wordList.addAll(Arrays.asList(words));
		    setOfShingles = archiveItShingle.computeShingles(wordList);
		    hashmapframe.put(url, new HashSet<>(setOfShingles));
		    wordList.clear();
		    setOfShingles.clear();
		    Elements element;
		    if(document.select("body, BODY") == null || document.select("body, BODY").first() == null || document.select("body").first() == null)
			continue;
		    else
			element = document.select("body, BODY").first().children();
		    PreparedStatement ps = conn.prepareStatement("INSERT INTO `lw_resource_archive_shingles` VALUES (?,?);", Statement.RETURN_GENERATED_KEYS);
		    ps.setString(1, htmlString.toString());
		    ps.setString(2, element.text());
		    //ps.execute();
		    ps.close();
		    ps = conn.prepareStatement("UPDATE `lw_resource_archiveurl` SET `htmltext`=?,`htmltags`=?, `httpstatuscode`=? WHERE `resource_id`=? AND `archive_url`=?;", Statement.RETURN_GENERATED_KEYS);
		    ps.setString(1, element.text());
		    ps.setString(2, htmlString.toString());
		    ps.setInt(3, response.statusCode());
		    ps.setInt(4, resource_id);
		    ps.setString(5, url);
		    //ps.execute();
		    ps.close();
		    String[] words1 = element.text().replaceAll("[!?,.]", "").split(" ");
		    hashmapstring.put(url, element.text());
		    wordList.addAll(Arrays.asList(words1));
		    setOfShingles = archiveItShingle.computeShingles(wordList);
		    hashmaptext.put(url, new HashSet<>(setOfShingles));
		    htmlString.setLength(0);
		}
		int i = 0;
		float text = 0, frame = 0, d = 0;
		float par = (float) 0.3;
		for(i = 0; i < listOfArchives.size() - 1; i++)
		{
		    text = archiveItShingle.computeJaccardIndex(hashmaptext.get(listOfArchives.get(i).getArchiveUrl()), hashmaptext.get(listOfArchives.get(i + 1).getArchiveUrl()));
		    frame = archiveItShingle.computeJaccardIndex(hashmapframe.get(listOfArchives.get(i).getArchiveUrl()), hashmapframe.get(listOfArchives.get(i + 1).getArchiveUrl()));
		    d = par * text + (1 - par) * frame;
		    Timestamp sqlDate1 = new Timestamp(listOfArchives.get(i).getTimestamp().getTime());
		    Timestamp sqlDate2 = new Timestamp(listOfArchives.get(i + 1).getTimestamp().getTime());
		    PreparedStatement ps = conn.prepareStatement("UPDATE `lw_resource_archive_jaccardindex` SET `jaccard_frame`=? , `jaccard_text`=? WHERE `resource_id`=? AND `timestamp1`=? AND `timestamp2`=?;", Statement.RETURN_GENERATED_KEYS);
		    ps.setInt(3, resource_id);
		    ps.setTimestamp(4, sqlDate1);
		    ps.setTimestamp(5, sqlDate2);
		    ps.setFloat(1, frame);
		    ps.setFloat(2, text);
		    //ps.execute();
		    if(d <= 0.6)
		    {
			setOfNearUniqueArchives.add(listOfArchives.get(i).getArchiveUrl());
			setOfNearUniqueArchives.add(listOfArchives.get(i + 1).getArchiveUrl());
		    }
		}
	    }
	    catch(SQLException ex)
	    {
		ex.printStackTrace();
	    }
	    listOfArchives.clear();
	}
	System.exit(0);
    }
}
