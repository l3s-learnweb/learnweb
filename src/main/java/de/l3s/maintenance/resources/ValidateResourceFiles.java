package de.l3s.maintenance.resources;

import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.maintenance.MaintenanceTask;

public class ValidateResourceFiles extends MaintenanceTask {

    @Override
    protected void init() {
        requireConfirmation = true;
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        final ResourceDao resourceDao = getLearnweb().getDaoProvider().getResourceDao();

        int multipleSameType = resourceDao.withHandle(handle -> handle.select("SELECT COUNT(*) FROM lw_file f JOIN lw_resource_file r USING (file_id) "
            + "WHERE EXISTS (SELECT 1 FROM lw_file f2 JOIN lw_resource_file r2 USING (file_id) WHERE f.`type` = f2.`type` AND r.resource_id = r2.resource_id AND f.file_id < f2.file_id)")
            .mapTo(Integer.class).one());
        log.info("Found {} redundant files, where exists multiple files of the same type per resource", multipleSameType);

        if (!dryRun) {
            resourceDao.withHandle(handle -> handle.execute("DELETE t1 FROM lw_resource_file t1 "
                + "WHERE EXISTS (SELECT 1 FROM (SELECT * FROM lw_file f JOIN lw_resource_file r USING (file_id) WHERE EXISTS ("
                + "SELECT 1 FROM lw_file f2 JOIN lw_resource_file r2 USING (file_id) "
                + "WHERE f.`type` = f2.`type` AND r.resource_id = r2.resource_id AND f.file_id < f2.file_id "
                + ")) t2 WHERE t1.resource_id = t2.resource_id AND t1.file_id = t2.file_id)"));
        }

        int notUsedFiles = resourceDao.withHandle(handle -> handle.select("SELECT COUNT(*) FROM lw_file f "
            + "WHERE `type` IN ('THUMBNAIL_SMALL','THUMBNAIL_MEDIUM','THUMBNAIL_LARGE','MAIN','ORIGINAL') "
            + "AND NOT EXISTS (SELECT 1 FROM lw_resource_file rf WHERE f.file_id = rf.file_id)")
            .mapTo(Integer.class).one());
        log.info("Found {} files not used in any resource", notUsedFiles);

        if (!dryRun) {
            resourceDao.withHandle(handle -> handle.execute("DELETE f FROM lw_file f "
                + "WHERE `type` IN ('THUMBNAIL_SMALL','THUMBNAIL_MEDIUM','THUMBNAIL_LARGE','MAIN','ORIGINAL') "
                + "AND NOT EXISTS (SELECT 1 FROM lw_resource_file rf WHERE f.file_id = rf.file_id)"));
        }
    }

    public static void main(String[] args) {
        new ValidateResourceFiles().start(args);
    }
}
