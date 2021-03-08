package de.l3s.maintenance.resources;

import java.net.URL;
import java.util.List;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.maintenance.MaintenanceTask;
import de.l3s.util.UrlHelper;

public class RegenerateThumbnails extends MaintenanceTask {

    @Override
    protected void init() {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        final ResourceDao resourceDao = getLearnweb().getDaoProvider().getResourceDao();
        List<Resource> imagesWithoutThumbnail = resourceDao.withHandle(handle -> handle.select("SELECT *  FROM lw_resource "
            + "WHERE deleted = 0 AND max_image_url IS NOT NULL AND max_image_url IS NOT NULL AND thumbnail0_file_id IS NULL "
            + "AND storage_type=2 and type in('video', 'image') AND online_status != 'OFFLINE'").map(new ResourceDao.ResourceMapper()).list());
        log.warn("Found {} image/video resources without thumbnails", imagesWithoutThumbnail.size());

        List<Resource> websitesWithoutThumbnail = resourceDao.withHandle(handle -> handle.select("SELECT * FROM lw_resource "
            + "WHERE storage_type = 2 AND type = 'website' AND deleted = 0 AND url NOT LIKE '%learnweb%' AND group_id != 1346 " // exclude Fact Check group
            + "AND online_status = 'UNKNOWN' and thumbnail0_file_id IS NULL ORDER BY resource_id DESC").map(new ResourceDao.ResourceMapper()).list());
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

    protected void generateThumbnailsForWebsite(Resource resource) {
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

        if (resource.getSmallThumbnail() == null) {
            log.debug("create thumbnail");
            try {
                getLearnweb().getResourcePreviewMaker().processWebsite(resource);
                resource.setFormat("text/html");
            } catch (Throwable t) {
                log.warn("Can't create thumbnail for url: {}", url, t);
            }
        }

        resource.save();
    }

    private void generateThumbnailsForMediaResource(Resource resource) throws Exception {
        String url = UrlHelper.validateUrl(resource.getMaxImageUrl());

        if (null == url || url.contains("unavailable")) {
            log.error("image not available {}, for resource {} {}", resource.getUrl(), resource.getId(), UrlHelper.validateUrl(resource.getUrl()));
            resource.setOnlineStatus(Resource.OnlineStatus.OFFLINE);
            resource.save();
            return;
        }

        getLearnweb().getResourcePreviewMaker().processImage(resource, new URL(url).openStream());
        resource.save();
    }

    public static void main(String[] args) {
        new RegenerateThumbnails().start(args);
    }
}
