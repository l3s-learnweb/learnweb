package de.l3s.learnweb.resource;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.office.ConverterService;
import de.l3s.learnweb.resource.office.converter.model.ConverterRequest;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
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
 * Helper for create preview for a Resource.
 *
 * @author Philipp Kemkes, Oleh Astappiev
 */
public class ResourcePreviewMaker {
    private static final Logger log = LogManager.getLogger(ResourcePreviewMaker.class);

    private static final int SIZE0_MAX_WIDTH = 150;
    private static final int SIZE0_MAX_HEIGHT = 120;
    private static final int SIZE1_WIDTH = 150;
    private static final int SIZE2_MAX_WIDTH = 300;
    private static final int SIZE2_MAX_HEIGHT = 220;
    private static final int SIZE3_MAX_WIDTH = 500;
    private static final int SIZE3_MAX_HEIGHT = 600;
    private static final int SIZE4_MAX_WIDTH = 1280;
    private static final int SIZE4_MAX_HEIGHT = 1024;

    // file numbers. Was part of older Learnweb versions. Maybe it can't be for later use

    private final Learnweb learnweb;
    private final FileManager fileManager;
    private final String websiteThumbnailService;
    private final String archiveThumbnailService;

    private FFprobe ffprobe;
    private FFmpegExecutor executor;

    public ResourcePreviewMaker(Learnweb learnweb) {
        this.learnweb = learnweb;
        this.fileManager = this.learnweb.getFileManager();
        this.archiveThumbnailService = learnweb.getProperties().getProperty("ARCHIVE_WEBSITE_THUMBNAIL_SERVICE");
        this.websiteThumbnailService = learnweb.getProperties().getProperty("WEBSITE_THUMBNAIL_SERVICE");

        try {
            String ffmpegPath = learnweb.getProperties().getProperty("FFMPEG_PATH");
            String ffprobePath = learnweb.getProperties().getProperty("FFPROBE_PATH");

            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            this.ffprobe = new FFprobe(ffprobePath);
            this.executor = new FFmpegExecutor(ffmpeg, this.ffprobe);
        } catch (IOException e) {
            log.error("Couldn't find ffmpeg library. " + Misc.getSystemDescription());
        }
    }

    public void processResource(Resource resource) throws IOException, SQLException {
        InputStream inputStream = null;
        try {
            // if a web resource is not a simple website then download it
            if (resource.getStorageType() == Resource.WEB_RESOURCE && resource.getType() != ResourceType.website && (resource.getSource() == ResourceService.bing || resource.getSource() == ResourceService.internet)) {
                File file = new File();
                file.setType(TYPE.FILE_MAIN);
                file.setName(resource.getFileName());
                file.setMimeType(resource.getFormat());
                fileManager.save(file, FileInspector.openStream(resource.getUrl()));
                resource.addFile(file);

                resource.setFileUrl(file.getUrl());
            }

            if (resource.getType() == ResourceType.website) {
                processWebsite(resource);
            } else if (resource.getFile(TYPE.FILE_MAIN) != null) { // resource.getStorageType() == Resource.LEARNWEB_RESOURCE &&
                inputStream = resource.getFile(TYPE.FILE_MAIN).getInputStream();
                processFile(resource, inputStream);
            } else if (resource.getStorageType() == Resource.WEB_RESOURCE && StringUtils.isNotEmpty(resource.getMaxImageUrl())) {
                inputStream = FileInspector.openStream(resource.getMaxImageUrl());
                processImage(resource, inputStream);
            } else {
                inputStream = FileInspector.openStream(resource.getUrl());
                processFile(resource, inputStream);
            }
        } catch (Throwable e) {
            log.error("Error in creating thumbnails from " + resource.getFormat() + " (detected type: " + resource.getType() + ") for resource: " + resource.getId(), e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void processFile(Resource resource, InputStream inputStream) throws IOException, SQLException {
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
                // TODO @astappiev: add default icons
                // Oleh: I think we don't need to store default icon in database,
                // instead we need to generate it "on the fly" when we load resource from db
                break;
            default:
                log.error("Can't create thumbnail. Don't know how to handle resource " + resource.getId() + ", type " + resource.getType());
        }
    }

    private void processOfficeDocument(Resource resource) {
        try {
            ConverterService converterService = learnweb.getConverterService();
            ConverterRequest request = converterService.createThumbnailConverterRequest(resource.getFile(TYPE.FILE_MAIN));
            InputStream thumbnailStream = converterService.convert(request);
            if (thumbnailStream != null) {
                Image image = new Image(thumbnailStream);
                createThumbnails(resource, image, false);
                thumbnailStream.close();
            }
        } catch (Exception e) {
            log.error("An error occurs during creating thumbnail for the document: resource_id=" + resource.getId() + "; file=" + resource.getFileUrl(), e);
        }
    }

    public void processImage(Resource resource, InputStream inputStream) throws IOException, SQLException {
        // process image
        Image img = new Image(inputStream);

        if (img.getWidth() > SIZE3_MAX_WIDTH || img.getHeight() > SIZE3_MAX_HEIGHT) {
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

    public void processWebsite(Resource resource) throws IOException, SQLException {
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

    public void processArchivedVersion(Resource resource, String archiveUrl) throws IOException, SQLException {
        URL thumbnailUrl = new URL(archiveThumbnailService + StringHelper.urlEncode(archiveUrl));

        // process image
        Image img = new Image(thumbnailUrl.openStream());

        File file = new File();
        file.setType(TYPE.THUMBNAIL_LARGE);
        file.setName("wayback_thumbnail.png");
        file.setMimeType("image/png");
        file = fileManager.save(file, img.getInputStream());

        if (file.getId() > 0) {
            learnweb.getArchiveUrlManager().updateArchiveUrl(file.getId(), resource.getId(), archiveUrl);
        }

        resource.addFile(file);
        resource.setThumbnail4(new Thumbnail(file.getUrl(), img.getWidth(), img.getHeight(), file.getId()));

        createThumbnails(resource, img, true);
    }

    public void processVideo(Resource resource) {
        File originalFile;
        FFmpegProbeResult ffProbeResult = null;
        try {
            if (resource.getStorageType() == Resource.LEARNWEB_RESOURCE && resource.getType() == ResourceType.video && (resource.getMediumThumbnail() == null || resource.getMediumThumbnail().getFileId() == 0)) {
                originalFile = resource.getFile(TYPE.FILE_MAIN);
                String inputPath = originalFile.getActualFile().getAbsolutePath();

                java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"), originalFile.getId() + "_thumbnails");
                if (!tmpDir.mkdir()) {
                    log.fatal("Couldn't create temp directory for thumbnail creation");
                }

                // get video details
                ffProbeResult = this.getFFProbe(inputPath);

                // take multiple frames at different positions from the video and use the largest (highest contrast) as preview image
                String bestImagePath = createVideoPreviewImage(ffProbeResult, tmpDir);

                // generate thumbnail
                Image img = new Image(new FileInputStream(bestImagePath));
                createThumbnails(resource, img, false);

                // clean up
                FileUtils.deleteDirectory(tmpDir);
            }
        } catch (Exception e) {
            log.error("An error occurs during creating thumbnail for a video: resource_id=" + resource.getId() + "; file=" + resource.getFileUrl(), e);
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
                fileManager.save(originalFile);
                resource.addFile(originalFile);

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
                resource.setUrl(convertedFile.getUrl());
                resource.setFormat("video/mp4");
            }
        } catch (Exception e) {
            log.error("An error occurred during video conversion " + resource.getId(), e);
        }
    }

    private FFmpegProbeResult getFFProbe(String mediaPath) throws IOException {
        return ffprobe.probe(mediaPath);
    }

    private void convertVideo(FFmpegProbeResult in, String outputMediaPath) throws IOException {
        FFmpegError error = in.getError();
        if (error != null) {
            log.error("{} - {}", error.code, error.string);
        }

        FFmpegFormat format = in.getFormat();
        log.info(String.format("Converting '%s' from format '%s' into mp4 format.", StringHelper.getNameFromPath(format.filename), format.format_long_name));

        FFmpegBuilder builder = new FFmpegBuilder().setInput(in).overrideOutputFiles(true).addOutput(outputMediaPath).setFormat("mp4").setVideoCodec("libx264").setVideoBitRate(format.bit_rate).done();

        this.executor.createJob(builder).run();
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
                log.warn("Couldn't create thumbnail at position " + seconds, e);
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

        this.executor.createJob(builder).run();
        log.info("Creating thumbnail done.");
    }

    public void processPdf(Resource resource, InputStream inputStream) throws IOException, SQLException {
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
                log.debug("Skip PDF page with errors; page: " + p + "; resource: " + resource);
            }
        }
    }

    private void createThumbnails(Resource resource, Image img, boolean croppedToAspectRatio) throws IOException, SQLException {
        int width = img.getWidth();
        int height = img.getHeight();
        Image thumbnail = null;

        try {
            thumbnail = img.getResized(SIZE0_MAX_WIDTH, SIZE0_MAX_HEIGHT, croppedToAspectRatio);
            File file = new File();
            file.setType(TYPE.THUMBNAIL_VERY_SMALL);
            file.setName("thumbnail0.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail0(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if (width < SIZE0_MAX_WIDTH && height < SIZE0_MAX_HEIGHT) { // than it makes no sense to create larger thumbnails from a small image
                return;
            }

            thumbnail = croppedToAspectRatio ? img.getCroppedAndResized(SIZE1_WIDTH, SIZE1_WIDTH) : img.getResizedToSquare2(SIZE1_WIDTH, 0.0);
            file = new File();
            file.setType(TYPE.THUMBNAIL_SQUARED);
            file.setName("thumbnail1.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail1(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if (width < SIZE1_WIDTH && height < SIZE1_WIDTH) {
                return;
            }

            thumbnail = img.getResized(SIZE2_MAX_WIDTH, SIZE2_MAX_HEIGHT, croppedToAspectRatio);
            file = new File();
            file.setType(TYPE.THUMBNAIL_SMALL);
            file.setName("thumbnail2.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail2(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));

            if (width < SIZE2_MAX_WIDTH && height < SIZE2_MAX_HEIGHT) {
                return;
            }

            thumbnail = img.getResized(SIZE3_MAX_WIDTH, SIZE3_MAX_HEIGHT, croppedToAspectRatio);
            file = new File();
            file.setType(TYPE.THUMBNAIL_MEDIUM);
            file.setName("thumbnail3.png");
            file.setMimeType("image/png");
            fileManager.save(file, thumbnail.getInputStream());
            resource.addFile(file);
            resource.setThumbnail3(new Thumbnail(file.getUrl(), thumbnail.getWidth(), thumbnail.getHeight(), file.getId()));
        } finally {
            if (thumbnail != null) {
                thumbnail.dispose();
            }
            img.dispose();
        }
    }

    public static class CreateThumbnailThread extends Thread {
        private final Resource resource;

        public CreateThumbnailThread(Resource resource) {
            log.debug("Create CreateThumbnailThread for " + resource);
            this.resource = resource;
        }

        @Override
        public void run() {
            try {
                ResourcePreviewMaker rpm = Learnweb.getInstance().getResourcePreviewMaker();
                resource.setOnlineStatus(Resource.OnlineStatus.PROCESSING);
                rpm.processResource(resource);
                resource.setOnlineStatus(Resource.OnlineStatus.ONLINE);
                resource.save();
            } catch (Exception e) {
                log.error("Error in CreateThumbnailThread " + e);
            }
        }
    }
}
