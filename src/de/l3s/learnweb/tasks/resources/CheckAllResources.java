package de.l3s.learnweb.tasks.resources;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;

/**
 * Reeds through all undeleted resources and performs arbitrary tests.
 *
 * @author Kemkes
 */
public class CheckAllResources {
    private static final Logger log = LogManager.getLogger(CheckAllResources.class);

    private static final Map<String, MutableInt> freq = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Learnweb learnweb = Learnweb.createInstance();

        final int batchSize = 5000;
        ResourceManager resourceManager = learnweb.getResourceManager();
        resourceManager.setReindexMode(true);

        for (int i = 0; true; i++) {
            log.debug("Load page: " + i);
            List<Resource> resources = resourceManager.getResourcesAll(i, batchSize);

            if (resources.isEmpty()) {
                log.debug("finished: last page");
                break;
            }

            log.debug("Process page: " + i);

            for (Resource resource : resources) {
                //log.debug(resource);
                checkMetadata(resource);
            }

        }
        learnweb.onDestroy();

        log.info("counts");
        for (Entry<String, MutableInt> entry : freq.entrySet()) {
            log.info(entry);
        }
    }

    private static void checkMetadata(Resource resource) throws SQLException {
        for (Entry<String, String> entry : resource.getMetadata().entrySet()) {
            if (entry.getValue() == null) {
                log.warn("entry has no value: " + entry); // TODO remove entry
                resource.getMetadata().remove(entry.getKey());
                resource.save();
                continue;
            }

            if (entry.getValue().indexOf(Resource.METADATA_SEPARATOR) != -1) {
                count(entry.getKey());
            }
        }
    }

    private static void count(String word) {
        MutableInt count = freq.get(word);
        if (count == null) {
            freq.put(word, new MutableInt());
        } else {
            count.increment();
        }
    }

    private static class MutableInt {
        int value = 1; // note that we start at 1 since we're counting

        public void increment() {
            ++value;
        }

        public int get() {
            return value;
        }

        @Override
        public String toString() {
            return Integer.toString(get());
        }
    }
}
