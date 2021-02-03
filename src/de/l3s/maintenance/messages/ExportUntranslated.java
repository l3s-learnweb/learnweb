package de.l3s.maintenance.messages;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.l3s.maintenance.MaintenanceTask;
import de.l3s.util.MessagesHelper;

/**
 * Prints a table of untranslated keys for each language.
 *
 * @author Oleh Astappiev
 */
public class ExportUntranslated extends MaintenanceTask {
    private static final String CSV_SEPARATOR = ";";
    private static final String[] CSV_HEADERS = {"key", "english", "translation", "description"};

    @Override
    protected void run(final boolean dryRun) throws Exception {
        Properties baseMessages = MessagesHelper.getMessagesForLocale(null);
        Properties xyMessages = MessagesHelper.getMessagesForLocale("xy");

        List<String> translateLocales = MessagesHelper.getLocales(Arrays.asList("en", "xy"));


        for (String locale : translateLocales) {
            Properties localeMessages = MessagesHelper.getMessagesForLocale(locale);
            ResultsPrinter printer = dryRun ? new ResultsPrinter(locale) : new CsvResultsPrinter(locale);

            for (Map.Entry<Object, Object> entry : baseMessages.entrySet()) {
                if (!localeMessages.containsKey(entry.getKey()) && !"unused".equals(xyMessages.get(entry.getKey()))) {
                    printer.write(entry.getKey(), entry.getValue(), localeMessages.get(entry.getKey()), xyMessages.get(entry.getKey()));
                }
            }

            printer.close();
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static class ResultsPrinter {
        protected final String locale;
        protected long counter = 0;

        ResultsPrinter(final String locale) {
            this.locale = locale;
        }

        void write(Object key, Object en, Object loc, Object xy) throws IOException {
            counter++;
            System.out.printf("%s %s %s %s%n", key, en, loc, xy);
        }

        public void close() throws IOException {
            System.out.printf("%nTotal keys untranslated in %s: %s%n", locale, counter);
        }
    }

    private static class CsvResultsPrinter extends ResultsPrinter {
        private final FileWriter csvWriter;

        CsvResultsPrinter(final String locale) throws IOException {
            super(locale);

            csvWriter = new FileWriter("missing_keys_" + locale + ".csv", StandardCharsets.UTF_8);
            csvWriter.append(String.join(CSV_SEPARATOR, CSV_HEADERS));
            csvWriter.append(System.lineSeparator());
        }

        @Override
        void write(Object key, Object en, Object loc, Object xy) throws IOException {
            counter++;

            csvWriter.append(String.join(CSV_SEPARATOR, new String[] {toString(key), toString(en), toString(loc), toString(xy)}));
            csvWriter.append(System.lineSeparator());
        }

        @Override
        public void close() throws IOException {
            csvWriter.flush();
            csvWriter.close();
        }

        private static String toString(Object obj) {
            return obj == null || "TODO".equals(obj) ? "" : String.valueOf(obj);
        }
    }

    public static void main(String[] args) {
        new ExportUntranslated().start(args);
    }
}
