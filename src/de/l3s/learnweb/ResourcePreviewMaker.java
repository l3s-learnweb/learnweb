package de.l3s.learnweb;

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

import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.FileInspector.FileInfo;
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

    public ResourcePreviewMaker(Learnweb learnweb)
    {
	this.learnweb = learnweb;
	this.fileManager = this.learnweb.getFileManager();
	this.fileInspector = new FileInspector();

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
	resource.setFileName(info.getFileName());
	resource.setFormat(info.getMimeType());
	resource.setType(type);
	resource.setDescription(StringHelper.shortnString(info.getTextContent(), 1400));
	resource.setMachineDescription(info.getTextContent());

	if(type.equals("pdf"))
	{
	    processPdf(resource, inputStream);
	}
	else if(type.equals("image"))
	{
	    processImage(resource, inputStream);
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

	createThumbnails(resource, img, true);
    }

    public void processVideo(Resource resource) throws IOException, SQLException
    {
	URL thumbnailUrl = new URL(videoThumbnailService + StringHelper.urlEncode(resource.getUrl()));

	// process image
	Image img = new Image(thumbnailUrl.openStream());

	createThumbnails(resource, img, false);
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

}
