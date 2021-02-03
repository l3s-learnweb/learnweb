package de.l3s.maintenance.messages;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import de.l3s.maintenance.MaintenanceTask;

/**
 * Uses the list of corrected translations provided by Marco to check which translations were not yet checked.
 *
 * The returned list represents translation keys which were not checked.
 *
 * @author Philipp Kemkes
 */
public class LanguageFileComparison extends MaintenanceTask {

    @Override
    protected void run(final boolean dryRun) throws Exception {
        Properties languageProperties = new Properties();
        languageProperties.load(LanguageFileComparison.class.getClassLoader().getResourceAsStream("de/l3s/learnweb/lang/messages.properties"));

        String csvFile = "test.csv";

        BufferedReader buffer = new BufferedReader(new FileReader(csvFile, StandardCharsets.UTF_8));
        String line;
        while ((line = buffer.readLine()) != null) {

            if (!line.startsWith("#") && line.endsWith("#")) {
                log.error("Corrupted file at line: {}", line);
                continue;
            }

            line = line.substring(1, line.length() - 1); // remove "#"

            String translation = languageProperties.getProperty(line);

            log.debug(line);

            if (translation == null) {
                log.error("Illegal key: {}", line);
                continue;
            }

            languageProperties.remove(line);
        }

        log.debug("unchecked keys:");
        for (Object key : languageProperties.keySet()) {
            String keyStr = (String) key;
            if (keyStr.startsWith("ArchiveSearch.") || keyStr.startsWith("language_")) {
                continue;
            }

            log.debug(key);
        }

        buffer.close();
    }

    public static void main(String[] args) {
        new LanguageFileComparison().start(args);
    }
}
