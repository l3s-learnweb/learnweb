package de.l3s.archiveSearch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
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
import org.jsoup.select.Elements;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.MementoClient;
import de.l3s.util.Image;
import de.l3s.util.StringHelper;

public class ArchiveItShingle
{
    private final MementoClient mementoclient;

    private final int w = 10; // the N-gram dimension i.e w words in a shingle

    public ArchiveItShingle()
    {
	mementoclient = new MementoClient(null);
    }

    private final Set<String> intersect = new HashSet<String>();
    private final Set<String> union = new HashSet<String>();

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

    public static void main(String[] args) throws IOException
    {
	String url = null;
	float d = 0;

	ArchiveItShingle archiveItShingle = new ArchiveItShingle();

	List<ArchiveUrl> listOfArchives = new LinkedList<ArchiveUrl>();
	listOfArchives = archiveItShingle.mementoclient.getArchiveItVersions(227, "http://www.conservateur.ca/");
	List<String> wordList = new ArrayList<String>();
	List<String> shingleList = new LinkedList<String>();

	Set<String> setOfShingles = new HashSet<String>();
	Set<String> setOfNearUniqueArchivesPair = new HashSet<String>();
	Set<String> setOfNearUniqueArchivesSequence = new HashSet<String>();
	Set<String> setOfNonUniqueUrls = new HashSet<String>();

	HashMap<String, Set<String>> hashmap = new LinkedHashMap<String, Set<String>>();
	for(ArchiveUrl archiveUrl : listOfArchives)
	{
	    wordList.clear();
	    setOfShingles.clear();
	    url = archiveUrl.getArchiveUrl();
	    Document document = Jsoup.connect(url).get(); //fetch the web pages
	    document.select("wb_div#wm-disclaim").remove(); //remove Archive disclaimer from html text
	    Elements element = document.select("body").first().children();
	    String[] words = element.text().replaceAll("[!?,.]", "").split(" ");
	    wordList.addAll(Arrays.asList(words));
	    for(int i = 0; i < wordList.size() - archiveItShingle.w; i++)
	    {
		shingleList = wordList.subList(i, i + archiveItShingle.w);
		setOfShingles.add(StringUtils.join(shingleList, " ").toLowerCase());
	    }
	    hashmap.put(url, new HashSet<>(setOfShingles));
	}
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
			d = archiveItShingle.computeIndex(entry1.getValue(), entry2.getValue());
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
	for(String str : setOfNearUniqueArchivesPair)
	    archiveItShingle.processWebsite(str, "pair-" + str.substring(34, 45));
	int i = 0;
	url = null;
	String key = listOfArchives.get(0).getArchiveUrl();
	for(i = 1; i < listOfArchives.size(); i++)
	{
	    url = listOfArchives.get(i).getArchiveUrl();
	    if(key != url && !setOfNearUniqueArchivesSequence.contains(url))
	    {
		d = archiveItShingle.computeIndex(hashmap.get(url), hashmap.get(key));
		if(d <= 0.5)
		{
		    setOfNearUniqueArchivesSequence.add(key.toString());
		    setOfNearUniqueArchivesSequence.add(url);
		    key = url;
		}
	    }
	}
	for(String str : setOfNearUniqueArchivesSequence)
	    archiveItShingle.processWebsite(str, "seq-" + str.substring(34, 45));
    }
}
