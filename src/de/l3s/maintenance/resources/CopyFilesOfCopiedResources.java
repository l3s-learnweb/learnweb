package de.l3s.maintenance.resources;

import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

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

    private long filesCount = 0;
    private long sizeBytes = 0;

    @Override
    public void init() {
        requireConfirmation = true;
    }

    @Override
    public void run(boolean dryRun) {
        List<Resource> resources = getLearnweb().getDaoProvider().getJdbi().withHandle(handle -> handle
            .select("SELECT r.* FROM lw_resource r JOIN lw_file f ON r.thumbnail0_file_id = f.file_id WHERE f.resource_id != r.resource_id LIMIT 100")
            .map(new ResourceDao.ResourceMapper()).list());

        log.info("Total affected {} resources", resources.size());

        for (Resource resource : resources) {
            List<File> originalFiles = getLearnweb().getDaoProvider().getFileDao().findByResourceId(resource.getOriginalResourceId());

            if (originalFiles == null || originalFiles.isEmpty()) {
                log.warn("No files of origin {} for resource {}!", resource.getOriginalResourceId(), resource.getId());
            } else {
                copyFiles(resource, originalFiles, dryRun);
            }
        }

        log.info("Total {} files fount, estimated size {}", filesCount, FileUtils.byteCountToDisplaySize(sizeBytes));
    }

    private void copyFiles(final Resource resource, final Collection<File> originalFiles, final boolean dryRun) {
        try {
            for (File file : originalFiles) {
                if (file.getType().in(File.TYPE.DOC_CHANGES, File.TYPE.DOC_HISTORY)) {
                    continue; // skip them
                }

                filesCount += 1;
                sizeBytes += FileUtils.sizeOf(file.getActualFile());

                if (!dryRun) {
                    File copyFile = new File(file);
                    copyFile.setResourceId(resource.getId());
                    getLearnweb().getDaoProvider().getFileDao().save(copyFile, file.getInputStream());
                    resource.addFile(copyFile);

                    if (file.getType() == File.TYPE.FILE_MAIN) {
                        if (resource.getUrl().equals(file.getUrl())) {
                            resource.setUrl(copyFile.getUrl());
                        }

                        if (resource.getFileUrl().equals(file.getUrl())) {
                            resource.setFileUrl(copyFile.getAbsoluteUrl());
                        }

                        resource.save();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during copying resource files {}", resource, e);
        }
    }

    public static void main(String[] args) {
        new CopyFilesOfCopiedResources().start(args);
    }
}
