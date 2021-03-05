package de.l3s.learnweb.resource;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.office.ConverterService;
import de.l3s.util.Image;
import de.l3s.util.StringHelper;
import de.l3s.util.UrlHelper;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegError;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

/**
 * Helper for create preview for a Resource.
 *
 * @author Philipp Kemkes, Oleh Astappiev
 */
public class ResourcePreviewMaker implements Serializable {
    private static final long serialVersionUID = -5259988131984139763L;
    private static final Logger log = LogManager.getLogger(ResourcePreviewMaker.class);

    private static final int THUMBNAIL_SMALL_WIDTH = 160;
    private static final int THUMBNAIL_SMALL_HEIGHT = 120;
    private static final int THUMBNAIL_MEDIUM_WIDTH = 280;
    private static final int THUMBNAIL_MEDIUM_HEIGHT = 210;
    private static final int THUMBNAIL_LARGE_WIDTH = 2048;
    private static final int THUMBNAIL_LARGE_HEIGHT = 1536;

    private final FileDao fileDao;

    private final String ffmpegPath;
    private final String ffprobePath;
    private final String websiteThumbnailService;
    private final String archiveThumbnailService;
    private final String officeConverterService;

    private transient FFprobe ffprobe;
    private transient FFmpegExecutor ffmpegExecutor;

    public ResourcePreviewMaker(final FileDao fileDao, final ConfigProvider configProvider) {
        this.fileDao = fileDao;

        this.ffmpegPath = configProvider.getProperty("ffmpeg_path");
        this.ffprobePath = configProvider.getProperty("ffprobe_path");
        this.archiveThumbnailService = configProvider.getProperty("archive_website_thumbnail_service");
        this.websiteThumbnailService = configProvider.getProperty("website_thumbnail_service");
        this.officeConverterService = configProvider.getProperty("onlyoffice_server_url") + "/ConvertService.ashx";
    }

    private FFprobe getFFprobe() throws IOException {
        if (ffprobe == null) {
            ffprobe = new FFprobe(ffprobePath);
        }
        return ffprobe;
    }

    private FFmpegExecutor getFFmpegExecutor() throws IOException {
        if (ffmpegExecutor == null) {
            ffmpegExecutor = new FFmpegExecutor(new FFmpeg(ffmpegPath), getFFprobe());
        }
        return ffmpegExecutor;
    }

    public void processResource(Resource resource) throws IOException {
        InputStream inputStream = null;
        try {
            // if a web resource is not a simple website then download it
            if (resource.getStorageType() == Resource.WEB_RESOURCE && resource.getType() != ResourceType.website
                && (resource.getSource() == ResourceService.bing || resource.getSource() == ResourceService.internet)) {
                File file = new File();
                file.setType(TYPE.FILE_MAIN);
                file.setName(resource.getFileName());
                file.setMimeType(resource.getFormat());
                fileDao.save(file, UrlHelper.getInputStream(resource.getUrl()));

                resource.addFile(file);
                resource.setFileUrl(file.getAbsoluteUrl());
            }

            if (resource.getType() == ResourceType.website) {
                processWebsite(resource);
            } else if (resource.getFile(TYPE.FILE_MAIN) != null) { // resource.getStorageType() == Resource.LEARNWEB_RESOURCE &&
                inputStream = resource.getFile(TYPE.FILE_MAIN).getInputStream();
                processFile(resource, inputStream);
            } else if (resource.getStorageType() == Resource.WEB_RESOURCE && StringUtils.isNotEmpty(resource.getMaxImageUrl())) {
                inputStream = UrlHelper.getInputStream(resource.getMaxImageUrl());
                processImage(resource, inputStream);
            } else if (resource.getType() != ResourceType.glossary && resource.getType() != ResourceType.survey) {
                inputStream = UrlHelper.getInputStream(resource.getUrl());
                processFile(resource, inputStream);
            }
        } catch (Throwable e) {
            log.error("Error creating thumbnails from {} (type: {}) for resource: {}", resource.getFormat(), resource.getType(), resource.getId(), e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void processFile(Resource resource, InputStream inputStream) throws IOException {
        switch (resource.getType()) {
            case image:
                processImage(resource, inputStream);
                break;
            case pdf:
                processPdf(resource, inputStream);
                break;
            case video:
                processVideo(resource);
                break;
            case document:
            case presentation:
            case spreadsheet:
                processOfficeDocument(resource);
                break;
            case text:
            case audio:
            case file:
                break;
            default:
                log.error("Can't create thumbnail. Don't know how to handle resource {}, type {}", resource.getId(), resource.getType());
        }
    }

    private void processOfficeDocument(Resource resource) {
        try {
            String thumbnailUrl = ConverterService.convert(officeConverterService, resource.getFile(TYPE.FILE_MAIN));
            InputStream thumbnailStream = UrlHelper.getInputStream(thumbnailUrl);
            if (thumbnailStream == null) {
                throw new IllegalStateException("Error during conversion : stream is null");
            }

            createThumbnails(resource, new Image(thumbnailStream), false);
            thumbnailStream.close();
        } catch (Exception e) {
            log.error("An error occurs during creating thumbnail for document: resource_id={}; file={}", resource.getId(), resource.getFileUrl(), e);
        }
    }

    public void processImage(Resource resource, InputStream inputStream) throws IOException {
        // process image
        Image img = new Image(inputStream);

        resource.setWidth(img.getWidth());
        resource.setHeight(img.getHeight());

        Image thumbnail = img.getResized(THUMBNAIL_LARGE_WIDTH, THUMBNAIL_LARGE_HEIGHT);
        File file = new File();
        file.setType(TYPE.THUMBNAIL_LARGE);
        file.setName("thumbnail4.png");
        file.setMimeType("image/png");
        fileDao.save(file, thumbnail.getInputStream());
        thumbnail.dispose();

        resource.addFile(file);
        resource.setThumbnail4(new Thumbnail(file));

        createThumbnails(resource, img, false);
    }

    public void processWebsite(Resource resource) throws IOException {
        URL thumbnailUrl = new URL(websiteThumbnailService + StringHelper.urlEncode(resource.getUrl()));

        // process image
        Image img = new Image(thumbnailUrl.openStream());

        File file = new File();
        file.setType(TYPE.THUMBNAIL_LARGE);
        file.setName("website.png");
        file.setMimeType("image/png");
        fileDao.save(file, img.getInputStream());

        resource.addFile(file);
        resource.setThumbnail4(new Thumbnail(file));

        createThumbnails(resource, img, true);
    }

    public void processArchivedVersion(Resource resource, String archiveUrl) throws IOException {
        URL thumbnailUrl = new URL(archiveThumbnailService + StringHelper.urlEncode(archiveUrl));

        // process image
        Image img = new Image(thumbnailUrl.openStream());

        File file = new File();
        file.setType(TYPE.THUMBNAIL_LARGE);
        file.setName("wayback_thumbnail.png");
        file.setMimeType("image/png");
        fileDao.save(file, img.getInputStream());

        resource.addFile(file);
        resource.setThumbnail4(new Thumbnail(file));

        createThumbnails(resource, img, true);
    }

    public void processVideo(Resource resource) {
        File originalFile;
        FFmpegProbeResult ffProbeResult = null;
        try {
            if (resource.getStorageType() == Resource.LEARNWEB_RESOURCE && resource.getType() == ResourceType.video
                && (resource.getMediumThumbnail() == null || resource.getMediumThumbnail().getFileId() == 0)) {
                originalFile = resource.getFile(TYPE.FILE_MAIN);
                String inputPath = originalFile.getActualFile().getAbsolutePath();

                java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"), originalFile.getId() + "_thumbnails");
                if (!tmpDir.mkdir()) {
                    log.fatal("Couldn't create temp directory for thumbnail creation");
                }

                // get video details
                ffProbeResult = this.getFFprobe().probe(inputPath);

                // take multiple frames at different positions from the video and use the largest (highest contrast) as preview image
                String bestImagePath = createVideoPreviewImage(ffProbeResult, tmpDir);

                // generate thumbnail
                Image img = new Image(new FileInputStream(bestImagePath));
                createThumbnails(resource, img, false);

                // clean up
                FileUtils.deleteDirectory(tmpDir);
            }
        } catch (Exception e) {
            log.error("An error occurs during creating thumbnail for a video: resource_id={}; file={}", resource.getId(), resource.getFileUrl(), e);
        }

        // TODO @astappiev: move it somewhere in one place with converting documents
        // convert videos that are not in mp4 format
        try {
            if (ffProbeResult == null) {
                return;
            }

            boolean isSupported = resource.getFormat().equals("video/mp4")
                && ffProbeResult.streams.stream().anyMatch(videoStream -> videoStream.codec_name.equals("h264"));

            if (resource.getStorageType() == Resource.LEARNWEB_RESOURCE && resource.getType() == ResourceType.video && !isSupported) {
                originalFile = resource.getFile(TYPE.FILE_MAIN);

                java.io.File tempVideoFile = java.io.File.createTempFile(originalFile.getId() + "_video_", ".mp4");

                String outputPath = tempVideoFile.getAbsolutePath();
                convertVideo(ffProbeResult, outputPath);

                // move original file
                originalFile.setType(TYPE.FILE_ORIGINAL);
                fileDao.save(originalFile);
                resource.addFile(originalFile);

                // create new file
                File convertedFile = new File();
                convertedFile.setType(TYPE.FILE_MAIN);
                convertedFile.setName(StringHelper.filenameChangeExt(originalFile.getName(), "mp4"));
                convertedFile.setMimeType("video/mp4");
                fileDao.save(convertedFile, new FileInputStream(outputPath));
                tempVideoFile.delete();

                // update resource files
                resource.addFile(convertedFile);
                resource.setFileName(convertedFile.getName());
                resource.setFileUrl(convertedFile.getAbsoluteUrl());
                resource.setUrl(convertedFile.getUrl());
                resource.setFormat("video/mp4");
            }
        } catch (Exception e) {
            log.error("An error occurred during video conversion {}", resource.getId(), e);
        }
    }

    private void convertVideo(FFmpegProbeResult in, String outputMediaPath) throws IOException {
        FFmpegError error = in.getError();
        if (error != null) {
            log.error("{} - {}", error.code, error.string);
        }

        FFmpegFormat format = in.getFormat();
        log.info(String.format("Converting '%s' from format '%s' into mp4 format.", StringHelper.getNameFromPath(format.filename), format.format_long_name));

        FFmpegBuilder builder = new FFmpegBuilder().setInput(in)
            .overrideOutputFiles(true)
            .addOutput(outputMediaPath)
            .setFormat("mp4")
            .setVideoCodec("libx264")
            .setVideoBitRate(format.bit_rate)
            .done();

        getFFmpegExecutor().createJob(builder).run();
        log.info("Converting done.");
    }

    private String createVideoPreviewImage(FFmpegProbeResult in, java.io.File tmpDir) {
        final int candidateCount = 5;
        String bestImagePath = null;
        long bestImageFileSize = 0;

        for (int i = 0; i < candidateCount; i++) {
            int seconds = (int) Math.pow(10, i);
            try {
                java.io.File tempThumbnailFile = java.io.File.createTempFile("image_" + i, ".jpg", tmpDir);
                String outputPath = tempThumbnailFile.getAbsolutePath();
                saveVideoThumbnail(in, outputPath, seconds);

                long fileSize = (new java.io.File(outputPath)).length();
                if (fileSize > bestImageFileSize) {
                    bestImageFileSize = fileSize;
                    bestImagePath = outputPath;
                }
            } catch (Exception e) {
                log.warn("Couldn't create thumbnail at position {}", seconds, e);
            }
        }

        return bestImagePath;
    }

    private void saveVideoThumbnail(FFmpegProbeResult in, String outputMediaPath, long seconds) throws IOException {
        FFmpegError error = in.getError();
        if (error != null) {
            log.error("{} - {}", error.code, error.string);
        }

        FFmpegFormat format = in.getFormat();
        log.info(String.format("Creating thumbnail for '%s'...", StringHelper.getNameFromPath(format.filename)));

        FFmpegBuilder builder = new FFmpegBuilder().setStartOffset(seconds, TimeUnit.SECONDS).setInput(in).addOutput(outputMediaPath).setFrames(1).done();

        getFFmpegExecutor().createJob(builder).run();
        log.info("Creating thumbnail done.");
    }

    public void processPdf(Resource resource, InputStream inputStream) throws IOException {
        PDDocument pdfDocument = PDDocument.load(inputStream);
        PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);

        // try page by page to get an image
        for (int p = 0, t = pdfDocument.getNumberOfPages(); p <= t; p++) {
            try {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(p, 300);
                Image image = new Image(bufferedImage);

                createThumbnails(resource, image, false);
                return; // stop as soon as we got one image
            } catch (IOException e) {
                // some PDFs with special graphics cause errors
                log.debug("Skip PDF page with errors; page: {}; resource: {}", p, resource);
            }
        }
    }

    private void createThumbnails(Resource resource, Image img, boolean croppedToAspectRatio) throws IOException {
        int width = img.getWidth();
        int height = img.getHeight();

        try {
            Image thumbnail = img.getCroppedAndResized(THUMBNAIL_SMALL_WIDTH, THUMBNAIL_SMALL_HEIGHT);
            File file = new File();
            file.setType(TYPE.THUMBNAIL_SMALL);
            file.setName("thumbnail0.png");
            file.setMimeType("image/png");
            fileDao.save(file, thumbnail.getInputStream());
            thumbnail.dispose();
            resource.addFile(file);
            resource.setThumbnail0(new Thumbnail(file));

            if (width < THUMBNAIL_SMALL_WIDTH && height < THUMBNAIL_SMALL_HEIGHT) { // than it makes no sense to create larger thumbnails from a small image
                return;
            }

            thumbnail = img.getResized(THUMBNAIL_MEDIUM_WIDTH, THUMBNAIL_MEDIUM_HEIGHT, croppedToAspectRatio);
            file = new File();
            file.setType(TYPE.THUMBNAIL_MEDIUM);
            file.setName("thumbnail2.png");
            file.setMimeType("image/png");
            fileDao.save(file, thumbnail.getInputStream());
            thumbnail.dispose();
            resource.addFile(file);
            resource.setThumbnail2(new Thumbnail(file));
        } finally {
            img.dispose();
        }
    }

    public static class CreateThumbnailThread extends Thread {
        private final Resource resource;

        public CreateThumbnailThread(Resource resource) {
            log.debug("Create CreateThumbnailThread for {}", resource);
            this.resource = resource;
        }

        @Override
        public void run() {
            try {
                resource.setOnlineStatus(Resource.OnlineStatus.PROCESSING);
                Learnweb.getInstance().getResourcePreviewMaker().processResource(resource);
                resource.setOnlineStatus(Resource.OnlineStatus.ONLINE);
                resource.save();
            } catch (Exception e) {
                log.error("Error in CreateThumbnailThread", e);
            }
        }
    }
}
