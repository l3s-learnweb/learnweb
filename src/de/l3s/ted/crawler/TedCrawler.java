package de.l3s.ted.crawler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.learnweb.User;
import de.l3s.learnweb.solrClient.SolrClient;

public class TedCrawler
{
    private static final Logger log = Logger.getLogger(TedCrawler.class);

    /*No TED ID mentioned on the Ted talk page*/
    //static Connection dbcon;
    public static final String CRAWL_FOR_NEW_VIDEOS = "crawler need to be run for only the newly added videos ";
    public static final String CRAWL_FOR_TRANSCRIPTS = "crawler is run for new transcripts that are added to old videos";
    //public static final String CRAWL_IF_NO_TRANSCRIPTS = "crawl videos which initially had no transcripts ";

    private ResourcePreviewMaker rpm;
    private SolrClient solr;
    private Group tedGroup;
    private User admin;

    /**
     * You should implement this function to specify whether the given url
     * should be crawled or not (based on your crawling logic).
     */
    //If the page URL matches any of these given conditions, dont crawl the page.
    /*public boolean neglectThisPage(Page page)
    {
        if(page.getWebURL().getURL().contains("?v2"))
            return false;
        else if(page.getWebURL().getURL().contains("?page"))
            return false;
        else if(page.getWebURL().getURL().contains("/browse"))
            return false;
        else if(page.getWebURL().getURL().contains("/transcript"))
            return false;
        else if(page.getWebURL().getURL().contains("/recommendations"))
            return false;
        else if(page.getWebURL().getURL().contains("/citations"))
            return false;
        else if(page.getWebURL().getURL().contains("/corrections"))
            return false;
        else
            return true;
    }*/

    //A function to call and parse the transcript pages.
    //It extracts the transcript values and calls the function for converting the parsed values to RDF and then inserts the data(value, ID, lang) into the database.
    public static void extractTranscriptElements(Document dc, int resourceId, String lang/*, ToRDFController rdfControllerobject*/)
    {
        try
        {
            //Insert the crawled and the parsed values to into the Database
            PreparedStatement pStmt = Learnweb.getInstance().getConnection().prepareStatement("INSERT DELAYED INTO `ted_transcripts_paragraphs`(`resource_id`, `language`,`starttime`,`paragraph`) VALUES(?,?,?,?)");

            pStmt.setInt(1, resourceId);
            pStmt.setString(2, lang);

            Element transcriptbody = dc.select(".talk-article__body").first();
            int preRollOffset = 11820;
            Elements elements = transcriptbody.getElementsByClass("talk-transcript__para");
            for(Element element : elements)
            {
                Element firstFragment = element.getElementsByClass("talk-transcript__fragment").get(0);
                int startTime = Integer.parseInt(firstFragment.attr("data-time")) + preRollOffset;
                pStmt.setInt(3, startTime);

                String text = element.getElementsByClass("talk-transcript__para__text").get(0).text();
                pStmt.setString(4, text);
                pStmt.executeUpdate();
            }
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }
    }

    //Class TED Crawler
    public TedCrawler()
    {
        rpm = Learnweb.getInstance().getResourcePreviewMaker();
        solr = Learnweb.getInstance().getSolrClient();
        try
        {
            tedGroup = Learnweb.getInstance().getGroupManager().getGroupById(862);
            admin = Learnweb.getInstance().getUserManager().getUser(7727);
        }
        catch(SQLException e)
        {
            log.error("unhandled error", e);
        }

    }

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4" + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

    /**
     * This function is called when a page is fetched and ready to be processed
     * by your program.
     **/
    /*@Override
    public boolean shouldVisit(Page referringPage, WebURL url)
    {
        String href = url.getURL().toLowerCase();

        return !FILTERS.matcher(href).matches() && href.contains("/talks/");
    }*/

    /*@Override
    public void visit(Page page)
    {
    
        if(!neglectThisPage(page))
            return; //dont crawl the page if the value of this function is true
    
        HashSet<String> languageSet = new HashSet<String>();
        HashSet<String> languageListfromDatabase = new HashSet<String>();
        String title = null;
        String description = null;
        String totalViews = null;
        int duration = 0;
        Date publishedAt = null;
        String keywords = "";
        String maxImageUrl = null;
        int imageHeight = 0;
        int imageWidth = 0;
    
        String decideWhatToCrawl = null;
        Document doc = null;
        String url = page.getWebURL().getURL();
    
        String sub[] = url.split("talks/");
        String slug = sub[1];
        int resourceId = 0;
    
        if(page.getParseData() instanceof HtmlParseData)
        {
            //HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            //String text = htmlParseData.getText();
            //String html = htmlParseData.getHtml();
            //
            //System.out.println("Text length: " + text.length());
            //System.out.println("Html length: " + html.length());
    
            //html = StringEscapeUtils.escapeHtml(html);
    
            try
            {
                // check the database to identify if the video has already been crawled or if any new transcripts are added to the video
                PreparedStatement pStmnt = Learnweb.getInstance().getConnection().prepareStatement("SELECT DISTINCT resource_id, language FROM ted_video JOIN ted_transcripts_paragraphs USING(resource_id) WHERE slug=?");
                pStmnt.setString(1, slug);
                ResultSet rs = pStmnt.executeQuery();
    
                while(rs.next())
                {
                    resourceId = rs.getInt(1);
                    String languageCode = rs.getString(2);
                    if(languageCode != null)
                    {
                        languageListfromDatabase.add(languageCode);
                        decideWhatToCrawl = CRAWL_FOR_TRANSCRIPTS;
                    }
    
                }
                if(decideWhatToCrawl == null)
                    decideWhatToCrawl = CRAWL_FOR_NEW_VIDEOS;
    
            }
            catch(SQLException e)
            {
                log.error("unhandled error", e);
            }
    
            try
            {
    
                doc = Jsoup.parse(new URL(url), 10000);
    
                if(decideWhatToCrawl == CRAWL_FOR_NEW_VIDEOS)
                {
                    //if the videos are new, crawl for the basic attributes such as title, speaker, transcripts and call the function to convert them to RDF and insert into the database
    
                    Element totalviewsel = doc.select(".talk-sharing__value").first();
                    totalViews = totalviewsel.text();
    
                    Element titleel = doc.select("meta[name=title]").first();
                    title = titleel.attr("content");
    
                    Element keywordsl = doc.select("meta[name=keywords]").first();
                    keywords = keywordsl.attr("content");
                    System.out.println(keywords);
    
                    Element imgLinkl = doc.select("meta[property=og:image]").first();
                    maxImageUrl = imgLinkl.attr("content");
    
                    Element imageHeightElement = doc.select("meta[property=og:image:height]").first();
                    imageHeight = Integer.parseInt(imageHeightElement.attr("content"));
    
                    Element imageWidthElement = doc.select("meta[property=og:image:width]").first();
                    imageWidth = Integer.parseInt(imageWidthElement.attr("content"));
                    System.out.println(maxImageUrl);
    
                    Element descriptionel = doc.select("meta[name=description]").first(); //can use meta tag "description" instead
                    description = descriptionel.attr("content");
    
                    Element durationElement = doc.select("meta[property=video:duration]").first();
                    duration = Integer.parseInt(durationElement.attr("content"));
    
                    Element releaseDate = doc.select("meta[property=video:release_date").first();
                    publishedAt = new Date(Integer.parseInt(releaseDate.attr("content")) * 1000L);
    
                    Elements tags = doc.select("meta[property=video:tag");
                    for(Element tag : tags)
                        keywords += tag + ",";
                    if(keywords.length() > 0)
                        keywords = keywords.substring(0, keywords.length() - 1);
                    Resource tedResource = new Resource();
                    tedResource.setTitle(title);
                    tedResource.setDescription(description);
                    tedResource.setUrl("http://www.ted.com/talks/" + slug);
                    tedResource.setSource("TED");
                    tedResource.setType(Resource.ResourceType.video);
                    tedResource.setDuration(duration);
                    tedResource.setMaxImageUrl(maxImageUrl);
                    tedResource.setCreationDate(publishedAt);
                    // the embeeded code is created on the fly in Resource.getEmbedded()
                    //tedResource.setEmbeddedRaw("<iframe src=\"http://embed.ted.com/talks/" + slug + ".html\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");
                    tedResource.setTranscript("");
                    try
                    {
                        rpm.processImage(tedResource, FileInspector.openStream(tedResource.getMaxImageUrl()));
                        tedResource.setGroup(tedGroup);
                        admin.addResource(tedResource);
                        solr.indexResource(tedResource);
                    }
                    catch(SQLException | SolrServerException e1)
                    {
                        e1.printStackTrace();
                    }
                    try
                    {
                        String preQueryStatement = "INSERT INTO `ted_video`(`resource_id`,`slug`, `title`, `description`, `viewed_count`, `published_at`, `photo1_url`, `photo1_width`,`photo1_height`,`tags`,`duration`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
                        PreparedStatement pStmnt = Learnweb.getInstance().getConnection().prepareStatement(preQueryStatement);
    
                        //pStmnt = dbcon.prepareStatement(preQueryStatement);
                        pStmnt.setInt(1, tedResource.getId());
                        pStmnt.setString(2, slug);
                        pStmnt.setString(3, title);
                        pStmnt.setString(4, description);
                        pStmnt.setString(5, totalViews);
                        pStmnt.setTimestamp(6, new Timestamp(publishedAt.getTime()));
                        pStmnt.setString(7, maxImageUrl);
                        pStmnt.setInt(8, imageWidth);
                        pStmnt.setInt(9, imageHeight);
                        pStmnt.setString(10, keywords);
                        pStmnt.setInt(11, duration);
                        int val = pStmnt.executeUpdate();
                        if(val == 1)
                        {
                            System.out.println("DB insert Success");
                        }
                        else
                            System.out.println("failed to insert");
                    }
                    catch(SQLException e)
                    {
                        log.error("unhandled error", e);
                    }
    
                }
    
                //if the videos are old, don't crawl for the basic attributes but only for the new transcripts added
                if(decideWhatToCrawl == CRAWL_FOR_TRANSCRIPTS)
                {
    
                    Elements transcriptlinkeles = doc.select("link[rel=alternate]");
                    if(transcriptlinkeles != null && transcriptlinkeles.size() > 0)
                    {
                        for(Element transcriptLinkElement : transcriptlinkeles)
                        {
                            String hrefLang = transcriptLinkElement.attr("hreflang");
                            if(hrefLang != null && !hrefLang.isEmpty() && !hrefLang.equals("x-default"))
                                languageSet.add(hrefLang);
                        }
    
                        Document langpage;
                        if(languageSet.equals(languageListfromDatabase))
                            return;
                        else
                        {
                            languageSet.removeAll(languageListfromDatabase);
                            for(String langCode : languageSet)
                            {
                                try
                                {
                                    langpage = Jsoup.parse(new URL("http://www.ted.com/talks/" + slug + "/transcript/?lang=" + langCode), 10000);
                                    extractTranscriptElements(langpage, resourceId, langCode);
                                }
                                catch(HttpStatusException e)
                                {
                                    log.error("unhandled error", e);
                                }
                            }
                        }
                    }
    
                }
    
            }
            catch(IOException e)
            {
                log.error("unhandled error", e);
            }
    
        }
    }*/
}
