package de.l3s.learnweb.i18n;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class MessagesBundleTest {
    private static final Logger log = LogManager.getLogger(MessagesBundleTest.class);

    @Test
    void getString() {
        MessagesBundle bundleEn = new MessagesBundle(Locale.of("en", "US", ""));
        assertEquals("Welcome to Learnweb", bundleEn.format("homepageTitle", "Learnweb"));

        // not existing language, fallback to default
        MessagesBundle bundleYY = new MessagesBundle(Locale.of("yy"));
        assertEquals("Welcome to Learnweb", bundleYY.format("homepageTitle", "Learnweb"));

        MessagesBundle bundleDe = new MessagesBundle(Locale.of("de"));
        assertEquals("Willkommen bei Learnweb", bundleDe.format("homepageTitle", "Learnweb"));

        MessagesBundle bundlePt = new MessagesBundle(Locale.of("pt"));
        assertEquals("Bem-vindo ao Learnweb", bundlePt.format("homepageTitle", "Learnweb"));
    }

    @Test
    void testLanguageVariants() {
        assertEquals("Hallo", new MessagesBundle(Locale.of("de")).format("greeting"));
        assertEquals("Hallo", new MessagesBundle(Locale.of("de", "AT")).format("greeting"));
        assertEquals("Hallo", new MessagesBundle(Locale.of("de", "DE")).format("greeting"));
        assertEquals("Hallo", new MessagesBundle(Locale.of("de", "DE")).format("greeting"));
    }

    @Test
    void shouldFormatArgumentsInRequestedLocale() {
        Date date = Date.from(LocalDate.of(2026, 10, 5).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // make the JVM default differ from the requested locales, so formatting with the default locale fails the English assertions
        Locale defaultFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
        try {
            assertEquals("Page 1,234", MessagesBundle.format(Locale.of("en", "US"), "page_number", 1234));
            assertEquals("Seite 1.234", MessagesBundle.format(Locale.of("de"), "page_number", 1234));
            assertTrue(MessagesBundle.format(Locale.of("en", "US"), "survey.answer_restricted_dates_between", date, date).contains("10/5/26"));
            assertTrue(new MessagesBundle(Locale.of("de")).format("survey.answer_restricted_dates_between", date, date).contains("05.10.26"));

            // the English text comes from the base bundle (Locale.ROOT), its arguments must still use the requested locale (not 2026-10-05)
            assertTrue(MessagesBundle.format(Locale.UK, "survey.answer_restricted_dates_between", date, date).contains("05/10/2026"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, defaultFormatLocale);
        }
    }

    @Test
    void userGenderTest() {
        ResourceBundle bundleEn = new MessagesBundle(Locale.of("en", "US", ""));
        assertEquals("Unassigned", bundleEn.getString("user.gender.UNASSIGNED"));
        assertEquals("Male", bundleEn.getString("user.gender.MALE"));
        assertEquals("Female", bundleEn.getString("user.gender.FEMALE"));
        assertEquals("Other", bundleEn.getString("user.gender.OTHER"));

        ResourceBundle bundleDe = new MessagesBundle(Locale.of("de"));
        assertEquals("Nicht ausgewählt", bundleDe.getString("user.gender.UNASSIGNED"));
        assertEquals("Männlich", bundleDe.getString("user.gender.MALE"));
        assertEquals("Weiblich", bundleDe.getString("user.gender.FEMALE"));
        assertEquals("Divers", bundleDe.getString("user.gender.OTHER"));
    }

    @Test
    void shouldNotThrownOnUnknownKey() {
        ResourceBundle bundleEn = new MessagesBundle(Locale.of("en"));
        assertEquals("not_existing_key", bundleEn.getString("not_existing_key"));
    }

    @Test
    void shouldFormatLiteralMessage() {
        // a literal message is used instead of a key, its placeholders must still be replaced
        assertEquals("The course 'Tom' has been deleted.", MessagesBundle.format(Locale.ENGLISH, "The course ''{0}'' has been deleted.", "Tom"));
        assertEquals("2 resources were skipped.", MessagesBundle.format(Locale.ENGLISH, "{0, choice, 1#{0} resource|1<{0} resources} were skipped.", 2));
    }

    @Test
    void sizeShouldBeEqual() {
        MessagesBundle bundle = new MessagesBundle(Locale.of("en"));
        MessagesBundle bundleDe = new MessagesBundle(Locale.of("de"));
        MessagesBundle bundlePt = new MessagesBundle(Locale.of("pt", "BR"));
        assertEquals(bundle.keySet().size(), bundleDe.keySet().size());
        assertEquals(bundle.keySet().size(), bundlePt.keySet().size());
    }

    @Test
    void performanceTest() {
        // Custom cache (in MessagesBundle): Elapsed time: 26 ms, 27 ms, 27 ms
        // Control cache (JDK ResourceBundle): Elapsed time: 54 ms, 55 ms, 52 ms
        // No cache: Elapsed time: 48281 ms, 46665 ms, 46623 ms

        // warmup, initial load, should be cached
        MessagesBundle bundle = new MessagesBundle(Locale.of("de", "DE"));

        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            bundle = new MessagesBundle(Locale.of("de", "DE")); // yes, the idea is to load the bundle on every step
            assertEquals("Hallo", bundle.getString("greeting"));
        }
        long elapsed = System.nanoTime() - start;
        log.info("Elapsed time: {} ms", elapsed / 1000000);
        assertTrue(elapsed < 200 * 1000000); // less than 60 ms (increased to 200 ms to pass on CI)
    }
}
