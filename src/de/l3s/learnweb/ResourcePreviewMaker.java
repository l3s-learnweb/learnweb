package de.l3s.learnweb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
import de.l3s.office.DocumentManager;
import de.l3s.office.FileUtility;
import de.l3s.office.ServiceConverter;
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

        // if this is a new file add it to the resource
        if(resource.getFile(TYPE.FILE_MAIN) == null)
        {
            File file = new File();
            file.setType(TYPE.FILE_MAIN);
            file.setName(info.getFileName());
            file.setMimeType(info.getMimeType());
            file.setDownloadLogActivated(true);
            fileManager.save(file, inputStream);
            if(shouldBeConverted(file.getName()))
            {

                File fileForConversion = createFileForConversion(file);
                inputStream = ServiceConverter.convert(file.getName(), fileForConversion.getUrl());
                if(inputStream == null)
                {
                    inputStream = ServiceConverter.convert(file.getName(), fileForConversion.getUrl());
                }
                fileManager.delete(file);
                fileManager.save(fileForConversion, inputStream);
                fillResource(resource, info, getRightTypeForConvertedFile(type, info), fileForConversion);
                inputStream = fileForConversion.getInputStream();
            }
            else
            {

                fillResource(resource, info, type, file);
                inputStream = file.getInputStream();
            }
        }

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
        else if(isDocFile(info, type))
        {
            try
            {
                InputStream wordPdf = null;
                if(inputStream != null)
                    wordPdf = ProcessOffice.processWord(resource, inputStream);
                if(wordPdf != null)
                    processPdf(resource, wordPdf);
            }
            catch(Exception e)
            {
                log.error("Error in creating thumbnails from Word " + resource.getFormat() + " for resource: " + resource.getId());
            }
        }
        else if(isPresentationFile(info))
        {
            try
            {
                BufferedImage img = null;
                if(inputStream != null)
                    img = ProcessOffice.processPPT(inputStream, resource);
                if(!img.equals(null))
                {
                    Image pptImg = new Image(img);
                    createThumbnails(resource, pptImg, false);
                }
            }
            catch(Exception e)
            {
                log.error("Error in creating thumbnails from ppt " + resource.getFormat() + " for resource: " + resource.getId());
            }
        }
        else if(isSpreadsheetFile(info))
        {
            try
            {
                InputStream xlPdf = null;
                if(inputStream != null)
                    xlPdf = ProcessOffice.processXls(inputStream, resource);
                if(xlPdf != null)
                    processPdf(resource, xlPdf);
            }
            catch(Exception e)
            {
                log.error("Error in creating thumbnails from xls " + resource.getFormat() + " for resource: " + resource.getId());
            }
        }
        if(inputStream != null)
            inputStream.close();
    }

    private boolean isSpreadsheetFile(FileInfo info)
    {
        return info.getMimeType().contains("excel") || info.getMimeType().contains("spreadsheet");
    }

    private boolean isPresentationFile(FileInfo info)
    {
        return info.getMimeType().contains("powerpoint") || info.getMimeType().contains("presentation");
    }

    private boolean isDocFile(FileInfo info, String type)
    {
        return type.equalsIgnoreCase("msword") || type.equalsIgnoreCase("doc") || info.getMimeType().contains("ms-word") || info.getMimeType().contains("officedocument.wordprocessingml.document");
    }

    private void fillResource(Resource resource, FileInfo info, String type, File file) throws SQLException
    {
        if(resource.getTitle() == null || resource.getTitle().length() == 0)
            resource.setTitle(info.getTitle());

        resource.addFile(file);
        resource.setUrl(file.getUrl());
        resource.setFileUrl(file.getUrl()); // for Loro resources the file url is different from the url
        resource.setFileName(file.getName());
        resource.setFormat(info.getMimeType());
        resource.setType(type);
        resource.setDescription(StringHelper.shortnString(info.getTextContent(), 1400));
        resource.setMachineDescription(info.getTextContent());
    }

    private String getRightTypeForConvertedFile(String type, FileInfo info)
    {
        if(isDocFile(info, type))
        {
            return "vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        else if(isSpreadsheetFile(info))
        {
            return "vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        else if(isPresentationFile(info))
        {
            return "vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        return type;
    }

    private File createFileForConversion(File source)
    {
        File destination = new File();
        //String fileUrl = "http://learnweb.l3s.uni-hannover.de" + source.getUrl().replace("/Learnweb-Tomcat/", "/").replace("http://localhost:8089", "");//TODO: DELETE IN PROD
        String fileType = FileUtility.getFileType(source.getName());
        String internalFileExt = DocumentManager.GetInternalExtension(fileType);
        destination.setName(source.getName().substring(0, source.getName().indexOf(".")) + internalFileExt);
        destination.setUrl(source.getUrl());
        destination.setType(source.getType());
        destination.setMimeType(source.getMimeType());
        destination.setDownloadLogActivated(true);
        return destination;
    }

    private boolean shouldBeConverted(String fileName)
    {
        return learnweb.getProperties().getProperty("files.docservice.convert-docs").contains(fileName.substring(fileName.indexOf(".")));
    }

    public void processImage(Resource resource, InputStream inputStream) throws IOException, SQLException
    {
        // process image
        Image img = new Image(inputStream);

        if(img.getWidth() > SIZE3_MAX_WIDTH || img.getHeight() > SIZE3_MAX_HEIGHT)
        {
            Image thumbnail = img.getResized(SIZE4_MAX_WIDTH, SIZE4_MAX_HEIGHT);
            File file = new File();
            file.setType(TYPE.THUMBNAIL_LARGE);
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
        file.setType(TYPE.THUMBNAIL_LARGE);
        file.setName("website.png");
        file.setMimeType("image/png");
        fileManager.save(file, img.getInputStream());

        resource.addFile(file);
        resource.setThumbnail4(new Thumbnail(file.getUrl(), img.getWidth(), img.getHeight(), file.getId()));

        createThumbnails(resource, img, true);
    }

    public void processArchivedVersion(Resource resource, String archiveUrl) throws IOException, SQLException
    {
        URL thumbnailUrl = new URL(archiveThumbnailService + StringHelper.urlEncode(archiveUrl));

        // process image
        Image img = new Image(thumbnailUrl.openStream());

        File file = new File();
        file.setType(TYPE.THUMBNAIL_LARGE);
        file.setName("wayback_thumbnail.png");
        file.setMimeType("image/png");
        file = fileManager.save(file, img.getInputStream());

        if(file.getId() > 0)
        {
            learnweb.getArchiveUrlManager().updateArchiveUrl(file.getId(), resource.getId(), archiveUrl);
        }

        resource.addFile(file);
        resource.setThumbnail4(new Thumbnail(file.getUrl(), img.getWidth(), img.getHeight(), file.getId()));

        createThumbnails(resource, img, true);
    }

    /**
     * This method is used to process an archived web page to
     * generate thumbnails specific for the CoverFlow Visualization
     */
    public void processArchiveWebsite(int resourceId, String url) throws IOException, SQLException
    {
        URL thumbnailUrl = new URL(archiveThumbnailService + StringHelper.urlEncode(url));
        Image img = new Image(thumbnailUrl.openStream());
        File file = new File();
        file.setResourceId(resourceId);
        file.setMimeType("image/png");

        int width = img.getWidth();
        int height = img.getHeight();
        if(width > SIZE3_MAX_WIDTH && height > SIZE3_MAX_HEIGHT)
        {
            img = img.getResized(SIZE3_MAX_WIDTH, SIZE3_MAX_HEIGHT, true);
            file.setType(TYPE.THUMBNAIL_MEDIUM);
            file.setName("wayback_thumbnail3.jpg");
            fileManager.save(file, img.getInputStream());
        }
        else
        {
            file.setType(TYPE.THUMBNAIL_LARGE);
            file.setName("wayback_thumbnail.jpg");
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
            // create a simple url for the video, the thumbnail service does not support some special chars in urls
            String url = videoThumbnailService + StringHelper.urlEncode(learnweb.getFileManager().createUrl(resource.getFile(TYPE.FILE_MAIN).getId(), "video.dat"));
            log.debug("Create video thumbnail: " + url);

            // get website thumbnail
            URL thumbnailUrl = new URL(url);
            Image img = new Image(thumbnailUrl.openStream());

            createThumbnails(resource, img, false);

            return;
        }
        catch(Exception e)
        {
            log.fatal("Can't create thumbnail for video. resource_id=" + resource + "; file=" + resource.getFileUrl(), e);
        }

        // use default image if we can't create one
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
            file.setType(TYPE.THUMBNAIL_VERY_SMALL);
            file.setName("thumbnail0.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail0(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if(width < SIZE0_MAX_WIDTH && height < SIZE0_MAX_HEIGHT) // than it makes no sense to create larger thumbnails from a small image
                return;

            thumbnail = croppedToAspectRatio ? img.getCroppedAndResized(SIZE1_WIDTH, SIZE1_WIDTH) : img.getResizedToSquare2(SIZE1_WIDTH, 0.0);
            file = new File();
            file.setType(TYPE.THUMBNAIL_SQUARD);
            file.setName("thumbnail1.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail1(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if(width < SIZE1_WIDTH && height < SIZE1_WIDTH)
                return;

            thumbnail = img.getResized(SIZE2_MAX_WIDTH, SIZE2_MAX_HEIGHT, croppedToAspectRatio);
            file = new File();
            file.setType(TYPE.THUMBNAIL_SMALL);
            file.setName("thumbnail2.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail2(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if(width < SIZE2_MAX_WIDTH && height < SIZE2_MAX_HEIGHT)
                return;

            thumbnail = img.getResized(SIZE3_MAX_WIDTH, SIZE3_MAX_HEIGHT, croppedToAspectRatio);
            file = new File();
            file.setType(TYPE.THUMBNAIL_MEDIUM);
            file.setName("thumbnail3.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail3(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));
        }
        finally
        {
            thumbnail.dispose();
            img.dispose();
        }
    }

    /* public void processWOrd(Resource resource, InputStream ip)
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
    }*/

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
                log.debug("Skip PDF page with errors; page: " + page + "; resource: " + resource);
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

    }

}
