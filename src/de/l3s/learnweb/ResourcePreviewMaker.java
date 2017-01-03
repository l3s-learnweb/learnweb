package de.l3s.learnweb;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
import de.l3s.learnwebBeans.AddResourceBean;
import de.l3s.util.Image;
import de.l3s.util.StringHelper;

public class ResourcePreviewMaker
{
    private final static Logger log = Logger.getLogger(ResourcePreviewMaker.class);

    private final static int BUFSIZE = 1024;
    private final static int SIZE0_MAX_WIDTH = 150;
    private final static int SIZE0_MAX_HEIGHT = 120;
    private final static int SIZE1_WIDTH = 150;
    private final static int SIZE2_MAX_WIDTH = 300;
    private final static int SIZE2_MAX_HEIGHT = 220;
    private final static int SIZE3_MAX_WIDTH = 500;
    private final static int SIZE3_MAX_HEIGHT = 600;
    private final static int SIZE4_MAX_WIDTH = 1280;
    private final static int SIZE4_MAX_HEIGHT = 1024;

    // file numbers. Was part of older Learnweb versions. Maybe it can't be for later use

    private final Learnweb learnweb;
    private final FileManager fileManager;
    private final FileInspector fileInspector;
    private final String websiteThumbnailService;
    private final String videoThumbnailService;
    private final String archiveThumbnailService;

    protected ResourcePreviewMaker(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        this.fileManager = this.learnweb.getFileManager();
        this.fileInspector = new FileInspector(learnweb);
        this.archiveThumbnailService = learnweb.getProperties().getProperty("ARCHIVE_WEBSITE_THUMBNAIL_SERVICE");
        this.websiteThumbnailService = learnweb.getProperties().getProperty("WEBSITE_THUMBNAIL_SERVICE");
        this.videoThumbnailService = learnweb.getProperties().getProperty("VIDEO_THUMBNAIL_SERVICE");
    }

    /**
     * Trys to extrat mime type, title and text from a file
     * 
     * @param inputStream
     * @param fileName
     * @return
     * @throws IOException
     */
    public FileInfo getFileInfo(InputStream inputStream, String fileName) throws IOException
    {
        return fileInspector.inspect(inputStream, fileName);
    }

    /**
     * Creates preview images if possible and adds them to the resource
     * 
     * @param resource
     * @param inputStream
     * @param fileName
     * @throws IOException
     * @throws SQLException
     */
    public void processFile(Resource resource, InputStream inputStream, FileInfo info) throws IOException, SQLException
    {
        String type = info.getMimeType().substring(0, info.getMimeType().indexOf("/"));
        if(type.equals("application"))
            type = info.getMimeType().substring(info.getMimeType().indexOf("/") + 1);

        File file = new File();
        file.setResourceFileNumber(File.ORIGINAL_FILE); // number 4 is reserved for the original file
        file.setName(info.getFileName());
        file.setMimeType(info.getMimeType());
        file.setDownloadLogActivated(true);
        fileManager.save(file, inputStream);
        inputStream = file.getInputStream();

        if(resource.getTitle() == null || resource.getTitle().length() == 0)
            resource.setTitle(info.getTitle());

        resource.addFile(file);
        resource.setUrl(file.getUrl());
        resource.setFileUrl(file.getUrl()); // for Loro resources the file url is different from the url
        resource.setFileName(info.getFileName());
        resource.setFormat(info.getMimeType());
        resource.setType(type);
        resource.setDescription(StringHelper.shortnString(info.getTextContent(), 1400));
        resource.setMachineDescription(info.getTextContent());

        if(type.equalsIgnoreCase("pdf"))
        {
            processPdf(resource, inputStream);
        }
        else if(type.equalsIgnoreCase("image"))
        {
            processImage(resource, inputStream);
        }
        else if(type.equalsIgnoreCase("video"))
        {
            processVideo(resource);
        }
        inputStream.close();
    }

    public void processImage(Resource resource, InputStream inputStream) throws IOException, SQLException
    {
        // process image
        Image img = new Image(inputStream);

        if(img.getWidth() > SIZE3_MAX_WIDTH || img.getHeight() > SIZE3_MAX_HEIGHT)
        {
            Image thumbnail = img.getResized(SIZE4_MAX_WIDTH, SIZE4_MAX_HEIGHT);
            File file = new File();
            file.setResourceFileNumber(5); // number 5 is reserved for the large thumbnail
            file.setName("thumbnail4.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            thumbnail.dispose();

            resource.addFile(file);
            resource.setThumbnail4(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));
        }
        createThumbnails(resource, img, false);
    }

    public void processWebsite(Resource resource) throws IOException, SQLException
    {
        resource.setType("text");
        resource.setFormat("text/html");

        URL thumbnailUrl = new URL(websiteThumbnailService + StringHelper.urlEncode(resource.getUrl()));

        // process image
        Image img = new Image(thumbnailUrl.openStream());

        File file = new File();
        file.setResourceFileNumber(5);
        file.setName("website.png");
        file.setMimeType("image/png");
        fileManager.save(file, img.getInputStream());

        resource.addFile(file);
        resource.setThumbnail4(new Thumbnail(file.getUrl(), img.getWidth(), img.getHeight(), file.getId()));

        createThumbnails(resource, img, true);
    }

    public void processArchiveWebsite(int resourceId, String url) throws IOException, SQLException
    {
        URL thumbnailUrl = new URL(archiveThumbnailService + StringHelper.urlEncode(url));
        Image img = new Image(thumbnailUrl.openStream());
        File file = new File();
        int width = img.getWidth();
        int height = img.getHeight();
        if(width > SIZE3_MAX_WIDTH && height > SIZE3_MAX_HEIGHT)
        {
            img = img.getResized(SIZE3_MAX_WIDTH, SIZE3_MAX_HEIGHT, true);
            file.setResourceFileNumber(3); // number 3 is reserved for the medium thumbnail
            file.setName("wayback_thumbnail3.jpg");
            file.setMimeType("image/png");
            fileManager.save(file, img.getInputStream());
        }
        else
        {
            file.setResourceFileNumber(5);
            file.setResourceId(resourceId);
            file.setName("wayback_thumbnail.jpg");
            file.setMimeType("image/png");
            file = fileManager.save(file, img.getInputStream());
        }

        if(file.getId() > 0)
        {
            learnweb.getArchiveUrlManager().updateArchiveUrl(file.getId(), resourceId, url);
        }
    }

    public void processVideo(Resource resource)
    {
        try
        {
            // get website thumbnail
            URL thumbnailUrl = new URL(videoThumbnailService + StringHelper.urlEncode(resource.getFileUrl()));

            Image img = new Image(thumbnailUrl.openStream());

            createThumbnails(resource, img, false);

            return;
        }
        catch(Exception e)
        {
            log.fatal("Can't create thumbnail for video. resource_id=" + resource + "; file=" + resource.getFileUrl(), e);
        }

        // use default image when we can't create one
        Thumbnail videoImage = new Thumbnail("../resources/resources/img/video.png", 200, 200);
        resource.setThumbnail0(videoImage.resize(150, 120));
        resource.setThumbnail1(videoImage.resize(150, 150));
        resource.setThumbnail2(videoImage);
        resource.setThumbnail3(videoImage);
        resource.setThumbnail4(videoImage);
    }

    public static void main(String[] ar)
    {

        try
        {
            Image img = new Image(FileInspector.openStream("http://www.filehippo.com/de/download/file/b9915e2b3dbaf63cc505890ee4cadd48302780c53bb04d55ecc8b3bd913ed7ce/"));

            ;

            FileOutputStream out = new FileOutputStream("c:\\ablage\\test.dat");
            IOUtils.copy(img.getInputStream(), out);
        }
        catch(Exception e)
        {
            log.fatal("Couldn't create an image of website: ", e);

        }

    }

    public void createThumbnails(Resource resource, Image img, boolean croppedToAspectRatio) throws IOException, SQLException
    {
        int width = img.getWidth();
        int height = img.getHeight();
        Image thumbnail = null;

        try
        {
            thumbnail = img.getResized(SIZE0_MAX_WIDTH, SIZE0_MAX_HEIGHT, croppedToAspectRatio);
            File file = new File();
            file.setResourceFileNumber(6); // number 6 is reserved for the smallest thumbnail
            file.setName("thumbnail0.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail0(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if(width < SIZE0_MAX_WIDTH && height < SIZE0_MAX_HEIGHT) // than it makes no sense to create larger thumbnails from a small image
                return;

            thumbnail = croppedToAspectRatio ? img.getCroppedAndResized(SIZE1_WIDTH, SIZE1_WIDTH) : img.getResizedToSquare2(SIZE1_WIDTH, 0.0);
            file = new File();
            file.setResourceFileNumber(1); // number 1 is reserved for the squared thumbnail
            file.setName("thumbnail1.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.setEmbeddedSize1Raw("<img src=\"" + Resource.createPlaceholder(1) + "\" width=\"" + thumbnail.getWidth() + "\" height=\"" + thumbnail.getHeight() + "\" />");
            resource.addFile(file);
            resource.setThumbnail1(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if(width < SIZE1_WIDTH && height < SIZE1_WIDTH)
                return;

            thumbnail = img.getResized(SIZE2_MAX_WIDTH, SIZE2_MAX_HEIGHT, croppedToAspectRatio);
            file = new File();
            file.setResourceFileNumber(2); // number 2 is reserved for the small thumbnail
            file.setName("thumbnail2.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail2(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if(width < SIZE2_MAX_WIDTH && height < SIZE2_MAX_HEIGHT)
                return;

            thumbnail = img.getResized(SIZE3_MAX_WIDTH, SIZE3_MAX_HEIGHT, croppedToAspectRatio);
            file = new File();
            file.setResourceFileNumber(3); // number 3 is reserved for the medium thumbnail
            file.setName("thumbnail3.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.setEmbeddedSize3Raw("<img src=\"" + Resource.createPlaceholder(3) + "\" width=\"" + thumbnail.getWidth() + "\" height=\"" + thumbnail.getHeight() + "\" />");
            resource.addFile(file);
            resource.setThumbnail3(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));
        }
        finally
        {
            thumbnail.dispose();
            img.dispose();
        }
    }

    public void processWOrd(Resource resource, InputStream ip)
    {
        XWPFDocument wordDocument = null;
        try
        {
            wordDocument = new XWPFDocument(ip);
        }
        catch(IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        POIXMLProperties props = wordDocument.getProperties();

        String thumbnail = props.getThumbnailFilename();
        if(thumbnail == null)
        {
            // No thumbnail
        }
        else
        {
            FileOutputStream fos = null;
            try
            {
                fos = new FileOutputStream("c:\\temp\\" + thumbnail);
            }
            catch(FileNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try
            {
                IOUtils.copy(props.getThumbnailImage(), fos);
            }
            catch(IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void processPdf(Resource resource, InputStream inputStream) throws IOException, SQLException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUFSIZE);
        IOUtils.copy(inputStream, out);
        inputStream.close();
        ByteBuffer buf = ByteBuffer.wrap(out.toByteArray());

        PDFFile pdfFile = new PDFFile(buf); // Create PDF Print Page
        int count = pdfFile.getNumPages();

        // try page by page to get an image
        for(int page = 1; page <= count; page++)
        {
            PDFPage p;

            try
            {
                p = pdfFile.getPage(page, true);
            }
            catch(Exception e)
            { // some pdfs with special graphics cause errors
                e.printStackTrace();
                return;
            }

            if(null == p)
                continue;

            int height = SIZE4_MAX_HEIGHT;
            int width = (int) Math.ceil(height * p.getAspectRatio());
            if(width > SIZE4_MAX_WIDTH)
            {
                width = SIZE4_MAX_WIDTH;
                height = (int) Math.ceil(width / p.getAspectRatio());
            }

            Image image = new Image(p.getImage(width, height, null, null, true, true));

            createThumbnails(resource, image, false);

            break; // stop as soon as we got one image
        }
        /*
        File file;
        
        if(page == 1)
        {
        Image thumbnail = image.getResizedToSquare2(SIZE1_WIDTH, 0.05);
        file = new File();
        file.setResourceFileNumber(1); // number 1 is reserved for the squared thumbnail
        file.setName("thumbnail1.png");
        file.setMimeType("image/png");
        fileManager.save(file, thumbnail.getInputStream());
        thumbnail.dispose();
        resource.setEmbeddedSize1Raw("<img src=\"" + Resource.createPlaceholder(1) + "\" width=\"" + thumbnail.getWidth() + "\" height=\"" + thumbnail.getHeight() + "\" />");
        resource.addFile(file);
        
        thumbnail = image.getResized(SIZE2_MAX_WIDTH, SIZE2_MAX_HEIGHT, 30);
        file = new File();
        file.setResourceFileNumber(2); // number 1 is reserved for the squared thumbnail
        file.setName("thumbnail2.png");
        file.setMimeType("image/png");
        fileManager.save(file, thumbnail.getInputStream());
        thumbnail.dispose();
        resource.setEmbeddedSize2Raw("<img src=\"" + Resource.createPlaceholder(2) + "\" width=\"" + thumbnail.getWidth() + "\" height=\"" + thumbnail.getHeight() + "\" />");
        resource.addFile(file);
        
        thumbnail = image.getResized(SIZE3_MAX_WIDTH, SIZE3_MAX_HEIGHT, 30);
        file = new File();
        file.setResourceFileNumber(3); // number 1 is reserved for the squared thumbnail
        file.setName("thumbnail3.png");
        file.setMimeType("image/png");
        fileManager.save(file, thumbnail.getInputStream());
        thumbnail.dispose();
        resource.addFile(file);
        //resource.setEmbeddedSize3Raw("<img src=\""+ Resource.createPlaceholder(3) +"\" width=\""+ thumbnail.getWidth() +"\" height=\""+ thumbnail.getHeight() +"\" id=\""+ key +"\" />");
        size3.append("<img src=\"" + Resource.createPlaceholder(3) + "\" width=\"" + thumbnail.getWidth() + "\" height=\"" + thumbnail.getHeight() + "\" id=\"" + key + "\" class=\"zoom_cursor\" ");
        //size3.append("<script type=\"text/javascript\">/*<![CDATA[* / showSlide3('"+ key +"',[0");
        size3.append("onclick=\"showSlide3('" + key + "',[0");
        }
        
        
        
        file = new File();
        file.setResourceFileNumber(fileCounter);
        file.setName("preview.png");
        file.setMimeType("image/png");
        fileManager.save(file, image.getInputStream());
        image.dispose();
        resource.addFile(file);
        size3.append(",'" + Resource.createPlaceholder(fileCounter) + "'");
        fileCounter++;
        
        }
        
        size3.append("]);\" />");
        // resource.setEmbeddedSize2Raw(size2.toString());
        resource.setEmbeddedSize3Raw(size3.toString());
        
        */
    }

    private static String getBetterSize(String imageUrl, int start, int end)
    {
        int size[] = { 1024, 800, 640, 500, 320, 240, 150, 100, 75 };
        int originalSize = Integer.parseInt(imageUrl.substring(start, end));
        int i = 0;
        String newUrl = null;
        while(i < size.length && size[i] > originalSize)
        {
            newUrl = imageUrl.substring(0, start) + size[i] + imageUrl.substring(end, imageUrl.length());
            if(AddResourceBean.checkUrl(newUrl) != null)
                return newUrl;
            i++;
        }
        return imageUrl;
    }

    public static String getBestImage(Resource resource)
    {
        String url = resource.getUrl();
        String imageUrl = null;
        Pattern pattern = Pattern.compile("<.+src=\"([^\"<>]+)\".*/>");
        if(null != resource.getEmbeddedSize1())
        {
            Matcher matcher = pattern.matcher(resource.getEmbeddedSize1());
            if(matcher.matches())
                imageUrl = matcher.group(1);
        }

        if(resource.getSource().equalsIgnoreCase("YouTube"))
        {
            url = AddResourceBean.checkUrl(url);
            if(url != null)
            {
                Pattern pattern1 = Pattern.compile("v[/=]([^&]+)");
                Matcher matcher1 = pattern1.matcher(url);
                if(matcher1.find())
                {
                    String videoId = matcher1.group(1);
                    imageUrl = "http://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                }
            }
        }
        else if(resource.getSource().equalsIgnoreCase("Google") && resource.getType().equalsIgnoreCase("Video"))
        {
            if(url != null)
            {
                Pattern pattern1 = Pattern.compile("youtube.com/watch%3Fv%3D([^&]+)");
                Matcher matcher1 = pattern1.matcher(url);
                if(matcher1.find())
                {
                    String videoId = matcher1.group(1);
                    imageUrl = "http://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                }
            }
        }
        else if(resource.getSource().equalsIgnoreCase("Vimeo"))
        {
            if(imageUrl != null)
            {
                Pattern pattern1 = Pattern.compile("([^_]+)_([0-9]+)\\.[a-zA-Z]+");
                Matcher matcher1 = pattern1.matcher(imageUrl);
                if(matcher1.matches())
                    imageUrl = getBetterSize(imageUrl, imageUrl.lastIndexOf('_') + 1, imageUrl.lastIndexOf('.'));
            }
        }
        else if(resource.getSource().equalsIgnoreCase("Ipernity"))
        {
            if(imageUrl != null)
            {
                Pattern pattern1 = Pattern.compile(".+\\.[0-9]+\\.[a-zA-Z]+");
                Matcher matcher1 = pattern1.matcher(imageUrl);
                if(matcher1.matches())
                    imageUrl = getBetterSize(imageUrl, imageUrl.lastIndexOf('.', imageUrl.lastIndexOf('.') - 1) + 1, imageUrl.lastIndexOf('.'));
            }
        }
        else if(resource.getSource().equalsIgnoreCase("Flickr"))
        {
            imageUrl = resource.getMaxImageUrl();
            if(imageUrl != null)
            {
                Pattern pattern1 = Pattern.compile("http://.+\\.[a-zA-Z]+");
                Matcher matcher1 = pattern1.matcher(imageUrl);
                if(matcher1.matches())
                {
                    char size[] = { 'o', 'b', 'c', 'z', 'n', 'm', 't', 'q', 's' };
                    int end = imageUrl.lastIndexOf('.');
                    char original = 'm';
                    if(imageUrl.charAt(end - 2) == '_')
                        original = imageUrl.charAt(end - 1);
                    int i = 0;
                    String newUrl = null;
                    while(i < size.length && size[i] != original)
                    {
                        if(size[i] == 'm')
                        {
                            newUrl = imageUrl.substring(0, end - 2) + imageUrl.substring(end, imageUrl.length());
                            newUrl = AddResourceBean.checkUrl(newUrl);
                            if(newUrl != null && !newUrl.contains("unavailable"))
                            {
                                imageUrl = newUrl;
                                break;
                            }
                        }
                        else
                        {
                            if(original == 'm')
                                newUrl = imageUrl.substring(0, end) + "_" + size[i] + imageUrl.substring(end, imageUrl.length());
                            else
                                newUrl = imageUrl.substring(0, end - 2) + "_" + size[i] + imageUrl.substring(end, imageUrl.length());

                            newUrl = AddResourceBean.checkUrl(newUrl);
                            if(newUrl != null && !newUrl.contains("unavailable"))
                            {
                                imageUrl = newUrl;
                                break;
                            }
                        }
                        i++;
                    }

                    if(size[i] == 'z')
                        imageUrl += "?zz=1";
                }
            }
        }
        else if(resource.getSource().equalsIgnoreCase("Desktop"))
        {
            if(null != resource.getEmbeddedSize4())
            {
                Matcher matcher = pattern.matcher(resource.getEmbeddedSize4());
                if(matcher.matches())
                    imageUrl = matcher.group(1);
            }
            else if(null != resource.getEmbeddedSize3())
            {
                Matcher matcher = pattern.matcher(resource.getEmbeddedSize3());
                if(matcher.matches())
                    imageUrl = matcher.group(1);
            }
        }
        else
        {
            // that's ok. This seem to be mostly web sites
            log.debug("unhandled resource " + resource);
            return null;

        }

        if(imageUrl == null) // why can't we get an images for this resource...
        {
            log.error("can't get image for " + resource);
        }

        if(url != null && url.startsWith("http://immediatenet.com"))
            return null;

        //log.debug(imageUrl);

        return imageUrl;

        /*
        if(imageUrl == null || AddResourceBean.checkUrl(imageUrl) == null)
        {
            resource.setMaxImageUrl("-1");
        }
        else
            resource.setMaxImageUrl(imageUrl);
        
        resource.save();
        */
    }

}
