package de.l3s.maintenance.resources;

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
        List<Resource> imagesWithoutThumbnail = resourceDao.withHandle(handle -> handle.select("SELECT * FROM lw_resource r "
            + "WHERE deleted = 0 AND max_image_url IS NOT NULL AND storage_type=2 AND type IN ('video', 'image') AND online_status != 'OFFLINE' "
            + "AND NOT EXISTS (SELECT 1 FROM lw_resource_file rf WHERE r.resource_id = rf.resource_id)")
            .map(new ResourceDao.ResourceMapper()).list());
        log.warn("Found {} image/video resources without thumbnails", imagesWithoutThumbnail.size());

        // exclude Fact Check group (1346)
        List<Resource> websitesWithoutThumbnail = resourceDao.withHandle(handle -> handle.select("SELECT * FROM lw_resource r "
            + "WHERE storage_type = 2 AND type = 'website' AND deleted = 0 AND url NOT LIKE '%learnweb%' AND online_status = 'UNKNOWN' AND group_id != 1346 "
            + "AND NOT EXISTS (SELECT 1 FROM lw_resource_file rf WHERE r.resource_id = rf.resource_id) ORDER BY resource_id DESC")
            .map(new ResourceDao.ResourceMapper()).list());
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

        url = UrlHelper.verifyUrl(url);
        if (url == null) {
            resource.setOnlineStatus(Resource.OnlineStatus.OFFLINE);
            resource.save();

            log.debug("offline");
            return;
        }

        resource.setOnlineStatus(Resource.OnlineStatus.ONLINE);
        log.debug("online");

        if (resource.getThumbnailSmall() == null) {
            log.debug("create thumbnail");
            try {
                getLearnweb().getResourcePreviewMaker().processWebsite(resource, resource.getUrl());
                resource.setFormat("text/html");
            } catch (Throwable t) {
                log.warn("Can't create thumbnail for url: {}", url, t);
            }
        }

        resource.save();
    }

    private void generateThumbnailsForMediaResource(Resource resource) throws Exception {
        String url = UrlHelper.verifyUrl(resource.getMaxImageUrl());

        if (null == url || url.contains("unavailable")) {
            log.error("image not available {}, for resource {} {}", resource.getMaxImageUrl(), resource.getId(), resource.getUrl());
            resource.setOnlineStatus(Resource.OnlineStatus.OFFLINE);
            resource.save();
            return;
        }

        getLearnweb().getResourcePreviewMaker().processImage(resource, UrlHelper.getInputStream(url));
        resource.save();
    }

    public static void main(String[] args) {
        new RegenerateThumbnails().start(args);
    }
}
