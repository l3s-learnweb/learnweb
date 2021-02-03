package de.l3s.maintenance.resources;

import java.net.URL;
import java.util.List;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.maintenance.MaintenanceTask;
import de.l3s.util.UrlHelper;

public class GenerateMissingThumbnails extends MaintenanceTask {

    private ResourceManager resourceManager;
    private ResourcePreviewMaker resourcePreviewMaker;

    @Override
    protected void init() throws Exception {
        resourceManager = getLearnweb().getResourceManager();
        resourcePreviewMaker = getLearnweb().getResourcePreviewMaker();
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        String queryMedia = "SELECT *  FROM `lw_resource` WHERE `deleted` = 0 AND `max_image_url` IS NOT NULL AND `max_image_url` != -1 AND thumbnail1_file_id = 0 AND storage_type=2 and type in('video', 'image') limit 10";
        List<Resource> imagesWithoutThumbnail = resourceManager.getResources(queryMedia, null);
        log.warn("Found {} image/video resources without thumbnails", imagesWithoutThumbnail.size());

        String queryWebsites = "SELECT * FROM `lw_resource` WHERE `storage_type` =2 AND `type` LIKE 'website' AND deleted =0 AND url NOT LIKE '%learnweb%' AND online_status = 'UNKNOWN' and thumbnail0_file_id = 0 ORDER BY resource_id DESC";
        List<Resource> websitesWithoutThumbnail = resourceManager.getResources(queryWebsites, null);
        log.warn("Found {} web resources without thumbnails", websitesWithoutThumbnail.size());

        if (!dryRun) {
            for (Resource resource : imagesWithoutThumbnail) {
                generateThumbnailsForMediaResource(resource);
            }

            for (Resource resource : websitesWithoutThumbnail) {
                generateThumbnailsForWebsite(resource);
            }
        }
    }

    protected void generateThumbnailsForWebsite(Resource resource) throws Exception {
        resource.setType(ResourceType.website);

        String url = resource.getUrl();
        log.debug("{}\t{}", resource.getId(), url);

        url = UrlHelper.validateUrl(url);

        if (url == null) {
            resource.setOnlineStatus(Resource.OnlineStatus.OFFLINE);
            resource.save();

            log.debug("offline");
            return;
        }
        resource.setOnlineStatus(Resource.OnlineStatus.ONLINE);
        log.debug("online");

        if (resource.getSmallThumbnail().getFileId() == 0) {
            log.debug("create thumbnail");
            try {
                resourcePreviewMaker.processWebsite(resource);
                resource.setFormat("text/html");
            } catch (Throwable t) {
                log.warn("Can't create thumbnail for url: {}", url, t);
            }
        }

        resource.save();
    }

    private void generateThumbnailsForMediaResource(Resource resource) throws Exception {
        log.debug(resource.getMaxImageUrl());
        String url = UrlHelper.validateUrl(resource.getMaxImageUrl());

        if (null == url || url.contains("unavailable")) {
            log.error("bild nicht erreichbar{}", resource.getUrl());
            url = UrlHelper.validateUrl(resource.getUrl());
            log.debug(url);
            return;
        }

        resourcePreviewMaker.processImage(resource, new URL(url).openStream());
        resource.save();
    }

    public static void main(String[] args) {
        new GenerateMissingThumbnails().start(args);
    }
}
