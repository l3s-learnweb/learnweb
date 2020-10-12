package de.l3s.maintenance.resources;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.FileManager;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Copies files for resources which was previously copied without copying files.
 *
 * @author Oleh Astappiev
 */
public final class CopyFilesOfCopiedResources extends MaintenanceTask {
    private ResourceManager resourceManager;
    private FileManager fileManager;

    private long filesCount = 0;
    private long sizeBytes = 0;

    @Override
    public void init() {
        fileManager = getLearnweb().getFileManager();
        resourceManager = getLearnweb().getResourceManager();
        resourceManager.setReindexMode(true);
    }

    @Override
    public void run(boolean dryRun) throws Exception {
        List<Resource> resources = resourceManager.getResources("SELECT * FROM lw_resource r WHERE original_resource_id > 0 AND "
            + "NOT EXISTS(SELECT 1 FROM lw_resource r2 WHERE r.original_resource_id = r2.resource_id AND r.file_url <> r2.file_url)", null);

        log.info("Total affected {} resources", resources.size());

        for (Resource resource : resources) {
            List<File> originalFiles = fileManager.getFilesByResource(resource.getOriginalResourceId());

            if (originalFiles == null || originalFiles.isEmpty()) {
                log.warn("No files of origin {} for resource {}!", resource.getOriginalResourceId(), resource.getId());
            } else {
                copyFiles(resource, originalFiles, dryRun);
            }
        }

        log.info("Total {} files fount, estimated size {}", filesCount, FileUtils.byteCountToDisplaySize(sizeBytes));
    }

    private void copyFiles(final Resource resource, final Collection<File> originalFiles, final boolean dryRun) throws SQLException {
        try {
            for (File file : originalFiles) {
                if (List.of(File.TYPE.THUMBNAIL_VERY_SMALL, File.TYPE.THUMBNAIL_SMALL, File.TYPE.THUMBNAIL_SQUARED,
                    File.TYPE.THUMBNAIL_MEDIUM, File.TYPE.THUMBNAIL_LARGE, File.TYPE.CHANGES, File.TYPE.HISTORY_FILE).contains(file.getType())) {
                    continue; // skip them
                }

                filesCount += 1;
                sizeBytes += FileUtils.sizeOf(file.getActualFile());

                if (!dryRun) {
                    File copyFile = new File(file);
                    copyFile.setResourceId(resource.getId());
                    getLearnweb().getFileManager().save(copyFile, file.getInputStream());
                    resource.addFile(copyFile);

                    if (file.getType() == File.TYPE.FILE_MAIN) {
                        if (resource.getUrl().equals(file.getUrl())) {
                            resource.setUrl(copyFile.getUrl());
                        }

                        if (resource.getFileUrl().equals(file.getUrl())) {
                            resource.setFileUrl(copyFile.getUrl());
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