package de.l3s.maintenance.resources;

import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Copies files for resources which was previously copied without copying files.
 *
 * @author Oleh Astappiev
 */
public final class CopyFilesOfCopiedResources extends MaintenanceTask {

    @Override
    public void init() {
        requireConfirmation = true;
    }

    @Override
    public void run(boolean dryRun) {
        ResourceDao resourceDao = getLearnweb().getDaoProvider().getResourceDao();
        List<Resource> resources = resourceDao.withHandle(handle -> handle
            .select("SELECT r.* FROM lw_resource r JOIN lw_file f ON r.thumbnail0_file_id = f.file_id WHERE f.resource_id != r.resource_id LIMIT 100")
            .map(new ResourceDao.ResourceMapper()).list());

        log.info("Total affected {} resources", resources.size());

        if (!dryRun) {
            int filesCount = 0;
            long sizeBytes = 0;

            for (Resource resource : resources) {
                List<File> originalFiles = getLearnweb().getDaoProvider().getFileDao().findByResourceId(resource.getOriginalResourceId());

                if (originalFiles == null || originalFiles.isEmpty()) {
                    log.warn("No files of origin {} for resource {}!", resource.getOriginalResourceId(), resource.getId());
                } else {
                    ImmutablePair<Integer, Long> stats = resourceDao.copyFiles(resource, originalFiles);
                    filesCount += stats.getLeft();
                    sizeBytes += stats.getRight();
                }
            }

            log.info("Total {} files fount, estimated size {}", filesCount, FileUtils.byteCountToDisplaySize(sizeBytes));
        }
    }

    public static void main(String[] args) {
        new CopyFilesOfCopiedResources().start(args);
    }
}
