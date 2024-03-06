package de.l3s.learnweb.i18n;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(elapsed < 150 * 1000000); // less than 60 ms (increased to 150 ms to pass on CI)
    }
}
