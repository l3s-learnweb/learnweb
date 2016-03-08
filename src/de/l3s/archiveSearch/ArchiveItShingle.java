package de.l3s.archiveSearch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.MementoClient;
import de.l3s.util.Image;
import de.l3s.util.StringHelper;

public class ArchiveItShingle
{
    private final MementoClient mementoclient;

    private final int w = 25; // the N-gram dimension i.e w words in a shingle

    public ArchiveItShingle()
    {
	mementoclient = new MementoClient(null);
    }

    private final Set<String> intersect = new HashSet<String>();
    private final Set<String> union = new HashSet<String>();

    /*compute the set of shingles given a 
     * list of words
     * */
    private Set<String> computeShingles(List<String> wordList)
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

    public float computeIndex(Set<String> set1, Set<String> set2)
    {
	intersect.clear();
	intersect.addAll(set1);
	intersect.retainAll(set2);
	union.clear();
	union.addAll(set1);
	union.addAll(set2);
	return (float) intersect.size() / union.size();
    }

    public void processWebsite(String url, String timestamp) throws IOException
    {
	URL thumbnailUrl = new URL("http://prometheus.kbs.uni-hannover.de/thumbnail/thumb_wb.php?url=" + StringHelper.urlEncode(url));
	// Process the Image
	Image img = new Image(thumbnailUrl.openStream());
	//java.io.File is the actual class File for creating files
	java.io.File actualFile = new java.io.File("F://DevTools//Work stuff//Crawler//Thumbnails//", timestamp + ".png");
	// copy the data into the file
	OutputStream outputStream = new FileOutputStream(actualFile);
	IOUtils.copy(img.getInputStream(), outputStream);
	outputStream.close();
    }

    public void processThumbnails(Set<String> set, String type) throws IOException
    {
	for(String str : set)
	    processWebsite(str, type + "-" + str.substring(34, 45));
    }

    /*used to get the html tags 
     * to calculate jaccard index 
     * by using the html DOM structure
    * */
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

    /*
     * Calculating the unique archives by comparing
     * each archive pair with each other*/
    public Set<String> computeUniqueArchivesByPair(HashMap<String, Set<String>> hashmap)
    {
	float d = 0;
	Set<String> setOfNonUniqueUrls = new HashSet<String>();
	Set<String> setOfNearUniqueArchivesPair = new HashSet<String>();
	for(Map.Entry<String, Set<String>> entry1 : hashmap.entrySet())
	{
	    for(Map.Entry<String, Set<String>> entry2 : hashmap.entrySet())
	    {
		if(entry1 != entry2 && !setOfNonUniqueUrls.contains(entry2.getKey()))
		{
		    if(setOfNearUniqueArchivesPair.contains(entry2.getKey()))
		    {

		    }
		    else
		    {
			d = computeIndex(entry1.getValue(), entry2.getValue());
			if(d <= 0.5)
			{
			    setOfNearUniqueArchivesPair.add(entry1.getKey());
			    setOfNearUniqueArchivesPair.add(entry2.getKey());
			}
		    }
		}
	    }
	    setOfNonUniqueUrls.add(entry1.getKey());
	}
	return setOfNearUniqueArchivesPair;
    }

    /*Calculating the unique achives by comparing each other by 
     * their order of insertion, starting from oldest archive to 
     * newest one.
     * 
     * */
    public Set<String> computeUniqueArchivesBySequence(HashMap<String, Set<String>> hashmap, List<ArchiveUrl> listOfArchives)
    {
	Set<String> setOfNearUniqueArchivesSequence = new HashSet<String>();
	int i = 0;
	float d = 0;
	String url = null;
	String key = listOfArchives.get(0).getArchiveUrl();
	for(i = 1; i < listOfArchives.size(); i++)
	{
	    url = listOfArchives.get(i).getArchiveUrl();
	    if(key != url && !setOfNearUniqueArchivesSequence.contains(url))
	    {
		d = computeIndex(hashmap.get(url), hashmap.get(key));
		System.out.println(d + " " + key + " " + url);
		if(d <= 0.7)
		{
		    setOfNearUniqueArchivesSequence.add(key.toString());
		    setOfNearUniqueArchivesSequence.add(url);
		    key = url;
		}
	    }
	}
	System.out.println();
	return setOfNearUniqueArchivesSequence;
    }

    public static void main(String[] args) throws IOException, SQLException
    {
	String url = null;
	final StringBuilder htmlString = new StringBuilder();

	ArchiveItShingle archiveItShingle = new ArchiveItShingle();

	List<ArchiveUrl> listOfArchives = new LinkedList<ArchiveUrl>();
	listOfArchives = archiveItShingle.mementoclient.getArchiveItVersions(227, "http://www.conservateur.ca/");
	List<String> wordList = new ArrayList<String>();

	Set<String> setOfShingles = new HashSet<String>();
	Set<String> setOfNearUniqueArchives = new HashSet<String>();

	HashMap<String, Set<String>> hashmapframe = new LinkedHashMap<String, Set<String>>();
	HashMap<String, Set<String>> hashmaptext = new LinkedHashMap<String, Set<String>>();

	for(ArchiveUrl archiveUrl : listOfArchives)
	{
	    wordList.clear();
	    setOfShingles.clear();
	    url = archiveUrl.getArchiveUrl();
	    System.out.println(url);
	    Document document = Jsoup.connect(url).get();
	    document.select("wb_div#wm-disclaim, script, style, head").remove();
	    document.traverse(archiveItShingle.processNode(htmlString));
	    String[] words = htmlString.toString().replaceAll("[!?,.]", "").split(" ");
	    wordList.addAll(Arrays.asList(words)); //remove Archive disclaimer from html text
	    setOfShingles = archiveItShingle.computeShingles(wordList);
	    hashmapframe.put(url, new HashSet<>(setOfShingles));
	    wordList.clear();
	    setOfShingles.clear();
	    Elements element = document.select("body").first().children();
	    String[] words1 = element.text().replaceAll("[!?,.]", "").split(" ");
	    wordList.addAll(Arrays.asList(words1));
	    setOfShingles = archiveItShingle.computeShingles(wordList);
	    hashmaptext.put(url, new HashSet<>(setOfShingles));
	}
	float text = 0, frame = 0, d = 0;
	float par = (float) 0.3;
	for(int i = 0; i < listOfArchives.size() - 1; i++)
	{
	    text = archiveItShingle.computeIndex(hashmaptext.get(listOfArchives.get(i).getArchiveUrl()), hashmaptext.get(listOfArchives.get(i + 1).getArchiveUrl()));
	    frame = archiveItShingle.computeIndex(hashmapframe.get(listOfArchives.get(i).getArchiveUrl()), hashmapframe.get(listOfArchives.get(i + 1).getArchiveUrl()));
	    d = par * text + (1 - par) * frame;
	    System.out.println(d + " " + listOfArchives.get(i).getArchiveUrl() + " " + listOfArchives.get(i + 1).getArchiveUrl());
	    if(d <= 0.6)
	    {
		setOfNearUniqueArchives.add(listOfArchives.get(i).getArchiveUrl());
		setOfNearUniqueArchives.add(listOfArchives.get(i + 1).getArchiveUrl());
	    }
	}
	archiveItShingle.processThumbnails(setOfNearUniqueArchives, "final");
    }
}
