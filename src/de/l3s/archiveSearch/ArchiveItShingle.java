package de.l3s.archiveSearch;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import com.github.tomtung.jsimhash.SimHashBuilder;
import com.github.tomtung.jsimhash.Util;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourcePreviewMaker;

public class ArchiveItShingle
{
    private static final Logger log = Logger.getLogger(ArchiveItShingle.class);

    private final int w = 25; // the N-gram dimension i.e w words in a shingle
    private SimHashBuilder simHashBuilder = new SimHashBuilder();

    //Threshold for hamming distance between simhashes of consecutive archived versions
    private int hammingDistanceThreshold = 4;

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

    /**
     * traverse HTML DOM structure
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
        LinkedList<Integer> duplicateShingleId = new LinkedList<Integer>();
        Connection conn = Learnweb.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT `shingle_id` FROM `lw_resource_archive_shingles` t1 join (SELECT * FROM `lw_resource_archiveurl` WHERE resource_id=?) t2 USING(shingle_id) group by `htmltext`, `htmltags` ORDER BY t1.shingle_id ASC");
        ps.setInt(1, resourceId);
        ResultSet rs = ps.executeQuery();
        while(rs.next())
            duplicateShingleId.add(rs.getInt("shingle_id"));
        duplicateShingleId.add(duplicateShingleId.getFirst() + size - 1);
        size = duplicateShingleId.getLast();
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
        log.debug(resourceId + " Amount of duplicates:" + t + " Total archive versions:" + d + " Percentage dublicates:" + (float) t * 100 / d);
    }

    /**
     * Detecting near duplicates based on sequence algorithm
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

    /**
     * Implementation of threshold grouping algorithm referenced in
     * ECIR14 by AlSum - Thumbnail Summarization Techniques for Web Archives
     */
    public LinkedList<ArchiveUrl> computeThresholdGroupingAlgo(LinkedList<ArchiveUrl> archiveUrls)
    {
        LinkedList<ArchiveUrl> selectedVersions = new LinkedList<ArchiveUrl>();

        for(int i = 1; i < archiveUrls.size(); i = i + 2)
        {
            long simhash1 = archiveUrls.get(i - 1).getSimhash();
            long simhash2 = archiveUrls.get(i).getSimhash();
            int hammingDistance = Util.hammingDistance(simhash1, simhash2);

            if(hammingDistance <= hammingDistanceThreshold)
                selectedVersions.add(archiveUrls.get(i - 1));
            else
            {
                selectedVersions.add(archiveUrls.get(i - 1));
                selectedVersions.add(archiveUrls.get(i));
            }
        }

        if(archiveUrls.size() % 2 != 0)
            selectedVersions.add(archiveUrls.getLast());

        if(selectedVersions.size() < archiveUrls.size())
            return computeThresholdGroupingAlgo(selectedVersions);
        else
            return selectedVersions;
    }

    public Set<String> computeUniqueArchivesByThresholdGrouping(LinkedList<ArchiveUrl> archiveUrls)
    {
        List<ArchiveUrl> selectedVersions = computeThresholdGroupingAlgo(archiveUrls);
        Set<String> selectedArchiveUrls = new LinkedHashSet<String>();
        for(ArchiveUrl version : selectedVersions)
            selectedArchiveUrls.add(version.getArchiveUrl());

        return selectedArchiveUrls;
    }

    public void computeThumbnailSummaryStatistics(int groupId) throws SQLException
    {
        Group group = Learnweb.getInstance().getGroupManager().getGroupById(groupId);
        int groupSize = group.getResources().size();

        float avgSum = 0;
        int count = 0, nonDupCount = 0, countSum = 0, dupCountSum = 0;//, evCount = 0;
        float textSim = 0.8f, frameSim = 0.8f;
        for(Resource r : group.getResources())
        {

            PreparedStatement ps = Learnweb.getInstance().getConnection().prepareStatement("SELECT COUNT(*) FROM `lw_resource_archiveurl` JOIN `lw_resource_archive_shingles` USING(shingle_id) WHERE `resource_id`=?");
            ps.setInt(1, r.getId());
            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                count = rs.getInt(1);
            }
            ps = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_resource_archiveurl` JOIN `lw_resource_archive_shingles` USING(shingle_id) WHERE `resource_id`=?  group by `shingle_id`");
            ps.setInt(1, r.getId());
            rs = ps.executeQuery();
            if(rs.last())
            {
                nonDupCount = rs.getRow();
                rs.beforeFirst();
            }

            List<ArchiveUrl> archiveUrls = new LinkedList<ArchiveUrl>();
            String htmlTags = null, htmlText = null;
            HashMap<String, Set<String>> hashmapFrame = new LinkedHashMap<String, Set<String>>();
            HashMap<String, Set<String>> hashmapText = new LinkedHashMap<String, Set<String>>();
            while(rs.next())
            {
                Date timestamp = new Date(rs.getTimestamp("timestamp").getTime());
                String url = rs.getString("archive_url");
                archiveUrls.add(new ArchiveUrl(url, timestamp));

                htmlTags = rs.getString("htmltags");
                String[] words = htmlTags.replaceAll("[!?,.]", "").split(" ");
                hashmapFrame.put(url, computeShingles(Arrays.asList(words)));

                htmlText = rs.getString("htmltext");
                words = htmlText.replaceAll("[!?,.]", "").split(" ");
                hashmapText.put(url, computeShingles(Arrays.asList(words)));
            }

            if(count > 0)
            {
                countSum += count;

                Set<String> uniqueUrls = computeUniqueArchivesBySequence(hashmapText, hashmapFrame, archiveUrls, r.getId(), textSim, frameSim);
                float avg = (float) uniqueUrls.size() / count;
                avgSum += avg;
                dupCountSum += uniqueUrls.size();
            }
            else
                groupSize--;

        }
        log.info("Average of entire group:" + (avgSum / groupSize));
        log.info("Average number of versions: " + ((float) countSum / groupSize));
        log.info("Average number of unique versions: " + ((float) dupCountSum / groupSize));

    }

    private long computeStringFingerprint(String s)
    {
        simHashBuilder.reset();
        int shinglingLength = 4; //As specified in AlSummarization Technique ECIR14
        s = s.replaceAll("[^\\w,]+", " ").toLowerCase();

        for(int i = 0; i <= s.length() - shinglingLength; i += 1)
        {
            simHashBuilder.addStringFeature(s.substring(i, i + shinglingLength));
        }
        return simHashBuilder.computeResult();
    }

    private void removeComments(Node node)
    {
        for(int i = 0; i < node.childNodes().size();)
        {
            Node child = node.childNode(i);
            if(child.nodeName().equals("#comment"))
                child.remove();
            else
            {
                removeComments(child);
                i++;
            }
        }
    }

    public long computeVersionFingerprint(String archiveUrl) throws IOException
    {
        Document document = null;
        long simhash = 0L;

        org.jsoup.Connection urlConnection = Jsoup.connect(archiveUrl).followRedirects(true).ignoreHttpErrors(true).timeout(10000);
        Response response = urlConnection.execute();
        if(response.statusCode() == 200)
        {
            document = urlConnection.get();
            document.select("wb_div#wm-disclaim, script, style, head").remove(); //remove Archive disclaimer from html text
            removeComments(document);
            simhash = computeStringFingerprint(document.toString());
        }

        return simhash;
    }

    public void computeSimhashForGroup(int groupId) throws SQLException
    {
        Group group = Learnweb.getInstance().getGroupManager().getGroupById(groupId);
        for(Resource r : group.getResources())
        {
            long start = System.currentTimeMillis();
            List<ArchiveUrl> versions = r.getArchiveUrls();
            PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE `lw_resource_archiveurl` SET `simhash`=? WHERE `resource_id`=? AND `archive_url`=?");
            for(ArchiveUrl v : versions)
            {
                try
                {
                    long simhash = computeVersionFingerprint(v.getArchiveUrl());
                    pStmt.setLong(1, simhash);
                    pStmt.setInt(2, r.getId());
                    pStmt.setString(3, v.getArchiveUrl());
                    pStmt.addBatch();

                }
                catch(IOException e)
                {
                    log.error(r.getId() + " ;" + v.toString(), e);
                }
            }
            pStmt.executeBatch();
            pStmt.close();
            log.info("Calculated simhashs for resource Id:" + r.getId() + " ; time taken:" + (System.currentTimeMillis() - start));

        }
    }

    public void extractingHTML(int resourceId, List<ArchiveUrl> archiveUrls) throws SQLException
    {
        for(ArchiveUrl version : archiveUrls)
        {
            String archiveUrl = version.getArchiveUrl();
            Document document = null;
            StringBuilder htmlString = new StringBuilder();
            try
            {
                org.jsoup.Connection urlConnection = Jsoup.connect(archiveUrl).ignoreHttpErrors(true).timeout(900000);
                Response response = urlConnection.execute();
                if(response.statusCode() == 200)
                    document = urlConnection.get();
                else
                {
                    PreparedStatement ps = Learnweb.getInstance().getConnection().prepareStatement("UPDATE `lw_resource_archiveurl` SET `httpstatuscode`=? WHERE `resource_id`=? AND `archive_url`=?");
                    ps.setInt(1, response.statusCode());
                    ps.setInt(2, resourceId);
                    ps.setString(3, archiveUrl);
                    ps.executeUpdate();
                    ps.close();
                    continue;
                }

                document.select("wb_div#wm-disclaim, script, style, head").remove(); //remove Archive disclaimer from html text
                document.traverse(processNode(htmlString));

                Elements element = null;
                Document[] framesDoc = null;
                if(document.select("body, BODY") == null || document.select("body, BODY").first() == null || document.select("body").first() == null)
                {
                    Elements frames = document.select("frame");
                    String[] framesSrc = new String[frames.size()];
                    int i = 0;
                    for(Element e : frames)
                    {
                        if(e.attr("src").startsWith("http"))
                            framesSrc[i++] = e.attr("src");
                        else
                            framesSrc[i++] = archiveUrl + e.attr("src");
                    }

                    framesDoc = new Document[frames.size()];
                    i = 0;
                    for(String src : framesSrc)
                    {
                        try
                        {
                            framesDoc[i++] = Jsoup.connect(src).get();
                        }
                        catch(IllegalArgumentException | IOException e)
                        {
                            log.error("Error while fetching frame source: ", e);
                        }
                    }

                    if(frames.size() == 0)
                        continue;
                }
                else
                    element = document.select("body, BODY").first().children();

                String textContent = "";
                if(element == null)
                {
                    for(Document f : framesDoc)
                    {
                        if(f != null)
                        {
                            f.select("wb_div#wm-disclaim, script, style, head").remove();
                            if(f.select("body, BODY") != null && f.select("body, BODY").first() != null)
                            {
                                f.select("body, BODY").first().children().traverse(processNode(htmlString));
                                textContent += f.select("body, BODY").first().children().text() + " ";
                            }
                        }
                    }
                }
                else
                    textContent = element.text();

                PreparedStatement ps = Learnweb.getInstance().getConnection().prepareStatement("INSERT INTO `lw_resource_archive_shingles`(htmltags,htmltext) VALUES (?,?);", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, htmlString.toString());
                ps.setString(2, textContent);
                ps.executeUpdate();

                ResultSet generatedKeys = ps.getGeneratedKeys();
                int shingle_id = 0;
                if(generatedKeys.next())
                    shingle_id = generatedKeys.getInt(1);

                ps.close();
                ps = Learnweb.getInstance().getConnection().prepareStatement("UPDATE `lw_resource_archiveurl` SET shingle_id=?, `httpstatuscode`=? WHERE `resource_id`=? AND `archive_url`=?;");
                ps.setInt(1, shingle_id);
                ps.setInt(2, response.statusCode());
                ps.setInt(3, resourceId);
                ps.setString(4, archiveUrl);
                ps.executeUpdate();
                ps.close();

                log.info(resourceId + " " + archiveUrl + " processed");
            }
            catch(IOException e)
            {
                log.error("Error while fetching archived version: " + archiveUrl, e);
            }
        }

    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException
    {
        ArchiveItShingle archiveItShingle = new ArchiveItShingle();
        Group g = Learnweb.createInstance("").getGroupManager().getGroupById(1132);
        /*float sumTotalAvgHammingDistances = 0f;
        int noAvgHammingDistances = 0;
        for(Resource r : g.getResources())
        {
            PreparedStatement ps = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM lw_resource_archiveurl WHERE resource_id=? and httpstatuscode = 200");
            ps.setInt(1, r.getId());
            ResultSet rs = ps.executeQuery();
            List<Long> simhashes = new ArrayList<Long>();
            float avgHammingDistance = 0f;
            int sumTotalHammingDistances = 0;
            int noHammingDistances = 0;
            int prevShingleId = 0;
            if(rs.next())
            {
                prevShingleId = rs.getInt("shingle_id");
                simhashes.add(rs.getLong("simhash"));
            }
        
            while(rs.next())
            {
                int shingleId = rs.getInt("shingle_id");
                if(shingleId == prevShingleId)
                    simhashes.add(rs.getLong("simhash"));
                else
                {
                    //System.out.println("Shingle Id:" + prevShingleId + "; simhashes size: " + simhashes.size());
                    prevShingleId = shingleId;
                    for(int i = 0; i < simhashes.size() - 1; i += 2)
                    {
                        int hammingDist = Util.hammingDistance(simhashes.get(i), simhashes.get(i + 1));
                        sumTotalHammingDistances += hammingDist;
                        noHammingDistances++;
                        //System.out.println(hammingDist);
                    }
                    simhashes.clear();
                    simhashes.add(rs.getLong("simhash"));
                }
            }
        
            avgHammingDistance = (float) sumTotalHammingDistances / noHammingDistances;
            if(!Float.isNaN(avgHammingDistance))
            {
                sumTotalAvgHammingDistances += avgHammingDistance;
                noAvgHammingDistances++;
        
                System.out.println("Average Hamming Distance For Exact Duplicates of " + r.getId() + " :" + avgHammingDistance);
            }
        }
        float avgHammingDistanceGroup = sumTotalAvgHammingDistances / noAvgHammingDistances;
        System.out.println("Average Hamming Distance for Exact Duplicates of Group:" + avgHammingDistanceGroup);
        */

        /*archiveItShingle.computeSimhashForGroup(1132);
        Group g = Learnweb.getInstance().getGroupManager().getGroupById(1132);
        for(Resource r : g.getResources())
        {
            if(r.getId() > 169897)
            {
                archiveItShingle.getDuplicateShingles(r.getId(), r.getArchiveUrls().size());
                archiveItShingle.generateThumbnails(r.getId());
            }
        }*/

        /*float sumTotalAvgs = 0f;
        int noAvgs = 0;
        
        for(Resource r : g.getResources())
        {
            LinkedList<ArchiveUrl> archiveUrls = new LinkedList<ArchiveUrl>();
            PreparedStatement ps = Learnweb.createInstance("").getConnection()
            .prepareStatement("SELECT * FROM `lw_resource_archiveurl` JOIN lw_resource_archive_shingles USING(shingle_id) WHERE `resource_id`=? AND httpstatuscode=200 GROUP BY htmltags, htmltext ORDER BY timestamp ASC");
            PreparedStatement ps = Learnweb.createInstance("").getConnection().prepareStatement("SELECT * FROM `lw_resource_archiveurl` WHERE `resource_id`=? AND httpstatuscode=200 ORDER BY timestamp ASC");
            //ps.setInt(1, 169896);
            ps.setInt(1, r.getId());
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                archiveUrls.add(new ArchiveUrl(rs.getString("archive_url"), rs.getDate("timestamp"), rs.getLong("simhash"), rs.getInt("shingle_id")));
            }
            LinkedList<ArchiveUrl> selectedUrls = archiveItShingle.computeThresholdGroupingAlgo(archiveUrls);
            System.out.println(archiveUrls.size());
            System.out.println(selectedUrls.size());
        
            float avg = (float) selectedUrls.size() / archiveUrls.size();
            if(!Float.isNaN(avg))
            {
                sumTotalAvgs += avg;
                noAvgs++;
            }
            System.out.println(r.getId() + " percentage of timemaps after filtering: " + avg);
        }
        float groupAvg = sumTotalAvgs / noAvgs;
        System.out.println(g.getId() + " " + groupAvg);*/

        for(float i = 0.5f; i >= 0.5f; i = i - 0.05f)
        {
            float sumTotalAvgs = 0f;
            int noAvgs = 0;
            for(Resource r : g.getResources())
            {
                String htmlText = null;
                String htmlTags = null;
                LinkedList<ArchiveUrl> archiveUrls = new LinkedList<ArchiveUrl>();
                HashMap<String, Set<String>> hashmapFrame = new HashMap<String, Set<String>>();
                HashMap<String, Set<String>> hashmapText = new HashMap<String, Set<String>>();

                float frameSim = i;
                float textSim = i;

                PreparedStatement ps = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_resource_archiveurl` JOIN `lw_resource_archive_shingles` USING(shingle_id) WHERE `resource_id`=? ORDER BY timestamp ASC");
                ps.setInt(1, r.getId());
                ResultSet rs = ps.executeQuery();

                while(rs.next())
                {
                    Date timestamp = new Date(rs.getTimestamp("timestamp").getTime());
                    String url = rs.getString("archive_url");
                    long simhash = rs.getLong("simhash");
                    archiveUrls.add(new ArchiveUrl(url, timestamp, simhash));

                    htmlTags = rs.getString("htmltags");
                    String[] words = htmlTags.replaceAll("[!?,.]", "").split(" ");
                    hashmapFrame.put(url, archiveItShingle.computeShingles(Arrays.asList(words)));

                    htmlText = rs.getString("htmltext");
                    words = htmlText.replaceAll("[!?,.]", "").split(" ");
                    hashmapText.put(url, archiveItShingle.computeShingles(Arrays.asList(words)));
                }
                if(archiveUrls.size() > 0)
                {
                    Set<String> selectedUrls = archiveItShingle.computeUniqueArchivesBySequence(hashmapText, hashmapFrame, archiveUrls, r.getId(), frameSim, textSim);
                    float avg = (float) selectedUrls.size() / archiveUrls.size();
                    if(!Float.isNaN(avg))
                    {
                        sumTotalAvgs += avg;
                        noAvgs++;
                    }

                    System.out.println(r.getId() + "; " + selectedUrls.size() + "; " + archiveUrls.size() + "; " + "; " + avg);
                }
            }
            float groupAvg = sumTotalAvgs / noAvgs;
            System.out.println(g.getId() + "; " + i + "; " + groupAvg);
            System.out.println();
        }

        System.exit(0);
    }
}
