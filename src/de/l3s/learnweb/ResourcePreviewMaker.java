package de.l3s.learnweb;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.office.ConverterService;
import de.l3s.office.converter.model.ConverterRequest;
import de.l3s.util.Image;
import de.l3s.util.Misc;
import de.l3s.util.StringHelper;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegError;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

/**
 * Helper for create preview for a Resource
 *
 * @author Philipp Kemkes, Oleh Astappiev
 */
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
    private final String websiteThumbnailService;
    private final String archiveThumbnailService;

    private FFprobe ffprobe;
    private FFmpegExecutor executor;

    protected ResourcePreviewMaker(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        this.fileManager = this.learnweb.getFileManager();
        this.archiveThumbnailService = learnweb.getProperties().getProperty("ARCHIVE_WEBSITE_THUMBNAIL_SERVICE");
        this.websiteThumbnailService = learnweb.getProperties().getProperty("WEBSITE_THUMBNAIL_SERVICE");

        try
        {
            String ffmpegPath = learnweb.getProperties().getProperty("FFMPEG_PATH");
            String ffprobePath = learnweb.getProperties().getProperty("FFPROBE_PATH");

            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            this.ffprobe = new FFprobe(ffprobePath);
            this.executor = new FFmpegExecutor(ffmpeg, this.ffprobe);
        }
        catch(IOException e)
        {
            log.error("Couldn't find ffmpeg library. " + Misc.getSystemDescription());
        }
    }

    public void processResource(Resource resource) throws IOException, SQLException
    {
        InputStream inputStream = null;
        try
        {
            // if a web resource is not a simple website then download it
            if(resource.getStorageType() == Resource.WEB_RESOURCE && !resource.getType().equals(Resource.ResourceType.website) && (resource.getSource().equals("Bing") || resource.getSource().equals("Internet")))
            {
                File file = new File();
                file.setType(TYPE.FILE_MAIN);
                file.setName(resource.getFileName());
                file.setMimeType(resource.getFormat());
                fileManager.save(file, FileInspector.openStream(resource.getUrl()));
                resource.addFile(file);

                resource.setFileUrl(file.getUrl());
            }

            if(resource.getType().equals(Resource.ResourceType.website))
            {
                processWebsite(resource);
            }
            else if(resource.getFile(TYPE.FILE_MAIN) != null) // resource.getStorageType() == Resource.LEARNWEB_RESOURCE && 
            {
                inputStream = resource.getFile(TYPE.FILE_MAIN).getInputStream();
                processFile(resource, inputStream);
            }
            else if(resource.getStorageType() == Resource.WEB_RESOURCE && StringUtils.isNotEmpty(resource.getMaxImageUrl()))
            {
                inputStream = FileInspector.openStream(resource.getMaxImageUrl());
                processImage(resource, inputStream);
            }
            else
            {
                inputStream = FileInspector.openStream(resource.getUrl());
                processFile(resource, inputStream);
            }
        }
        catch(Throwable e)
        {
            log.error("Error in creating thumbnails from " + resource.getFormat() + " for resource: " + resource.getId(), e);
        }
        finally
        {
            if(inputStream != null)
            {
                inputStream.close();
            }
        }
    }

    private void processFile(Resource resource, InputStream inputStream) throws IOException, SQLException
    {
        if(resource.getType().equals(Resource.ResourceType.image))
        {
            processImage(resource, inputStream);
        }
        else if(resource.getType().equals(Resource.ResourceType.pdf))
        {
            processPdf(resource, inputStream);
        }
        else if(resource.getType().equals(Resource.ResourceType.video))
        {
            processVideo(resource);
        }
        else if(resource.getType().equals(Resource.ResourceType.document) || resource.getType().equals(Resource.ResourceType.presentation) && !resource.getSource().equalsIgnoreCase("Slideshare") || resource.getType().equals(Resource.ResourceType.spreadsheet))
        {
            processOfficeDocument(resource);
        }
        else
        {
            log.error("Can't create thumbnail. Don't know how to handle resource " + resource.getId() + ", type " + resource.getType());
        }
    }

    private void processOfficeDocument(Resource resource)
    {
        try
        {
            ConverterService converterService = learnweb.getConverterService();
            ConverterRequest request = converterService.createThumbnailConverterRequest(resource.getFile(TYPE.FILE_MAIN));
            InputStream thumbnailStream = converterService.convert(request);
            if(thumbnailStream != null)
            {
                Image image = new Image(thumbnailStream);
                createThumbnails(resource, image, false);
                thumbnailStream.close();
            }
        }
        catch(IOException | SQLException e)
        {
            log.error("An error occurs during creating thumbnail for the document: resource_id=" + resource.getId() + "; file=" + resource.getFileUrl(), e);
        }

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
    /*
    public void processArchiveWebsite(int resourceId, String url) throws IOException, SQLException
    {
    
    What's the difference to the usual website thumbnail generation?
    Let's discuss this before this method is used again.
    
    
        URL thumbnailUrl = new URL(archiveThumbnailService + StringHelper.urlEncode(url));
    
        // process image
        Image img = new Image(thumbnailUrl.openStream());
    
        File file = new File();
        file.setResourceId(resourceId);
        file.setMimeType("image/png");
    
        if(img.getWidth() > SIZE3_MAX_WIDTH && img.getHeight() > SIZE3_MAX_HEIGHT)
        {
            img = img.getResized(SIZE3_MAX_WIDTH, SIZE3_MAX_HEIGHT, true);
            file.setType(TYPE.THUMBNAIL_MEDIUM);
            file.setName("wayback_thumbnail3.jpg");
        }
        else
        {
            file.setType(TYPE.THUMBNAIL_LARGE);
            file.setName("wayback_thumbnail.jpg");
        }
    
        file = fileManager.save(file, img.getInputStream());
    
        if(file.getId() > 0)
        {
            learnweb.getArchiveUrlManager().updateArchiveUrl(file.getId(), resourceId, url);
        }
    
    }
    */

    public void processVideo(Resource resource)
    {
        File originalFile = null;
        try
        {
            if(resource.getStorageType() == Resource.LEARNWEB_RESOURCE && resource.getType().equals(Resource.ResourceType.video) && (resource.getThumbnail2() == null || resource.getThumbnail2().getFileId() == 0))
            {
                originalFile = resource.getFile(TYPE.FILE_MAIN);

                java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"));
                java.io.File tempThumbnailFile = java.io.File.createTempFile(originalFile.getId() + "_thumbnail_", ".jpg", tmpDir);

                String inputPath = originalFile.getActualFile().getAbsolutePath();
                String outputPath = tempThumbnailFile.getAbsolutePath();
                saveVideoThumbnail(inputPath, outputPath, 1);

                // generate thumbnail
                Image img = new Image(new FileInputStream(outputPath));
                createThumbnails(resource, img, false);
                tempThumbnailFile.delete();
            }

            //            // create a simple url for the video, the thumbnail service does not support some special chars in urls
            //            String url = videoThumbnailService + StringHelper.urlEncode(learnweb.getFileManager().createUrl(resource.getFile(TYPE.FILE_MAIN).getId(), "video.dat"));
            //            log.debug("Create video thumbnail: " + url);
            //
            //            // get website thumbnail
            //            Image img = new Image(new URL(url).openStream());
            //            createThumbnails(resource, img, false);
        }
        catch(Exception e)
        {
            log.error("An error occurs during creating thumbnail for a video: resource_id=" + resource.getId() + "; file=" + resource.getFileUrl(), e);
        }

        // TODO Oleh: move it somewhere in one place with converting documents
        // convert videos that are not in mp4 format
        try
        {
            if(resource.getStorageType() == Resource.LEARNWEB_RESOURCE && resource.getType().equals(Resource.ResourceType.video) && !resource.getFormat().equals("video/mp4"))
            {
                originalFile = resource.getFile(TYPE.FILE_MAIN);

                java.io.File tempVideoFile = java.io.File.createTempFile(originalFile.getId() + "_video_", ".mp4");

                String inputPath = originalFile.getActualFile().getAbsolutePath();
                String outputPath = tempVideoFile.getAbsolutePath();
                convertVideo(inputPath, outputPath);

                // move original file
                originalFile.setType(TYPE.FILE_ORIGINAL);
                fileManager.save(originalFile);

                // create new file
                File convertedFile = new File();
                convertedFile.setType(TYPE.FILE_MAIN);
                convertedFile.setName(StringHelper.filenameChangeExt(originalFile.getName(), "mp4"));
                convertedFile.setMimeType("video/mp4");
                fileManager.save(convertedFile, new FileInputStream(outputPath));
                tempVideoFile.delete();

                // update resource files
                resource.addFile(convertedFile);
                resource.setFileName(convertedFile.getName());
                resource.setFileUrl(convertedFile.getUrl());
                resource.setFormat("video/mp4");
            }
        }
        catch(Exception e)
        {
            log.error("An error occurs during converting video " + resource.getId(), e);
        }
    }

    private FFmpegProbeResult getFFProbe(String mediaPath) throws IOException
    {
        return ffprobe.probe(mediaPath);
    }

    private void convertVideo(String inputMediaPath, String outputMediaPath) throws IOException
    {
        convertVideo(this.getFFProbe(inputMediaPath), outputMediaPath);
    }

    private void convertVideo(FFmpegProbeResult in, String outputMediaPath) throws IOException
    {
        FFmpegError error = in.getError();
        if(error != null)
        {
            log.error(error);
        }

        FFmpegFormat format = in.getFormat();
        log.info(String.format("Converting '%s' from format '%s' into mp4 format.", StringHelper.getNameFromPath(format.filename), format.format_long_name));

        FFmpegBuilder builder = new FFmpegBuilder().setInput(in).overrideOutputFiles(true).addOutput(outputMediaPath).setFormat("mp4").setVideoCodec("libx264").setVideoBitRate(format.bit_rate).done();

        this.executor.createJob(builder).run();
        log.info("Converting done.");
    }

    private void saveVideoThumbnail(String inputMediaPath, String outputMediaPath, long seconds) throws IOException
    {
        saveVideoThumbnail(this.getFFProbe(inputMediaPath), outputMediaPath, seconds);
    }

    private void saveVideoThumbnail(FFmpegProbeResult in, String outputMediaPath, long seconds) throws IOException
    {
        FFmpegError error = in.getError();
        if(error != null)
        {
            log.error(error);
        }

        FFmpegFormat format = in.getFormat();
        log.info(String.format("Creating thumbnail for '%s'...", StringHelper.getNameFromPath(format.filename)));

        FFmpegBuilder builder = new FFmpegBuilder().setStartOffset(seconds, TimeUnit.SECONDS).setInput(in).addOutput(outputMediaPath).setFrames(1).done();

        this.executor.createJob(builder).run();
        log.info("Creating thumbnail done.");
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
            catch(NoClassDefFoundError | Exception e)
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

    private void createThumbnails(Resource resource, Image img, boolean croppedToAspectRatio) throws IOException, SQLException
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
            file.setType(TYPE.THUMBNAIL_SQUARED);
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
}
