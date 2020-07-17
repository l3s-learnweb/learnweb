package de.l3s.maintenance.resources;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.maintenance.MaintenanceTask;

/**
 * Reeds through all undeleted resources and performs arbitrary tests.
 *
 * @author Philipp Kemkes
 */
public class CheckAllResources extends MaintenanceTask {
    private static final int batchSize = 5000;
    private static final Map<String, Long> freq = new HashMap<>();

    private ResourceManager resourceManager;

    @Override
    protected void init() {
        resourceManager = getLearnweb().getResourceManager();
        resourceManager.setReindexMode(true);
    }

    @Override
    protected void run(final boolean dryRun) throws Exception {
        for (int i = 0; true; i++) {
            List<Resource> resources = resourceManager.getResourcesAll(i, batchSize);
            if (resources.isEmpty()) {
                log.debug("finished: no more resources");
                break;
            }

            log.debug("Processing page: {}", i);

            for (Resource resource : resources) {
                checkMetadata(resource, dryRun);
            }
        }

        log.info("Counts: {}", StringUtils.join(freq));
    }

    private void checkMetadata(Resource resource, final boolean dryRun) throws SQLException {
        for (Entry<String, String> entry : resource.getMetadata().entrySet()) {
            if (entry.getValue() == null) {
                log.warn("entry has no value: {}", entry);

                if (!dryRun) {
                    // remove entry if value is null
                    resource.getMetadata().remove(entry.getKey());
                    resource.save();
                }
                continue;
            }

            if (entry.getValue().indexOf(Resource.METADATA_SEPARATOR) != -1) {
                freq.merge(entry.getKey(), 1L, Long::sum);
            }
        }
    }

    public static void main(String[] args) {
        new CheckAllResources().start(args);
    }
}
