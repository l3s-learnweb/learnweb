package messages;

import static messages.MessagesTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class ExportMessages {
    private static final String[] CSV_HEADERS = {"key", "english", "translation", "description"};

    private static CSVPrinter printer;

    @Test
    void exportMissingKeys() throws IOException {
        Properties baseMessages = getMessagesForLocale(null);
        Properties xyMessages = getMessagesForLocale("xy");

        assertAll(getLocales(Arrays.asList("en", "xy")).stream().map(locale -> () -> {
            Properties localeMessages = getMessagesForLocale(locale);

            baseMessages.forEach((key, value) -> {
                if (!localeMessages.containsKey(key)) {
                    writeCSV(locale, key, value, localeMessages.get(key), xyMessages.get(key));
                }
            });

            closeCSV();
        }));
    }

    private static void writeCSV(String locale, Object key, Object en, Object loc, Object xy) {
        if (!"unused".equalsIgnoreCase((String) xy)) {
            try {
                if ("TODO".equals(xy)) {
                    xy = null;
                }

                getCSVPrinter(locale).printRecord(key, en, loc, xy);
            } catch (IOException e) {
                fail("Can't write to CSVPrinter", e);
            }
        }
    }

    private static void closeCSV() {
        if (printer != null) {
            try {
                printer.close();
                printer = null;
            } catch (IOException e) {
                fail("Can't close CSVPrinter", e);
            }
        }
    }

    private static CSVPrinter getCSVPrinter(String locale) {
        if (printer == null) {
            try {
                FileWriter out = new FileWriter("missing_keys_" + locale + ".csv", StandardCharsets.UTF_8);
                printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADERS));
            } catch (IOException e) {
                fail("Can't create CSVPrinter", e);
            }
        }

        return printer;
    }
}
