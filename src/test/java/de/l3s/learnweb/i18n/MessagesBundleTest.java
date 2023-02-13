package de.l3s.learnweb;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import de.l3s.learnweb.i18n.MessagesBundle;

class MessagesBundleTest {

    @Test
    void getString() {
        MessagesBundle bundleEn = new MessagesBundle(new Locale("en", "US", ""));
        assertEquals("Welcome to Learnweb", bundleEn.format("homepageTitle", "Learnweb"));

        // not existing language, fallback to default
        MessagesBundle bundleYY = new MessagesBundle(new Locale("yy"));
        assertEquals("Welcome to Learnweb", bundleYY.format("homepageTitle", "Learnweb"));

        MessagesBundle bundleDe = new MessagesBundle(new Locale("de"));
        assertEquals("Willkommen bei Learnweb", bundleDe.format("homepageTitle", "Learnweb"));

        MessagesBundle bundlePt = new MessagesBundle(new Locale("pt"));
        assertEquals("Bem-vindo ao Learnweb", bundlePt.format("homepageTitle", "Learnweb"));
    }

    @Test
    void testLanguageVariants() {
        assertEquals("Hallo", new MessagesBundle(new Locale("de")).format("greeting"));
        assertEquals("Hallo", new MessagesBundle(new Locale("de", "AT")).format("greeting"));
        assertEquals("Hallo", new MessagesBundle(new Locale("de", "DE")).format("greeting"));
        assertEquals("Hallo", new MessagesBundle(new Locale("de", "DE")).format("greeting"));
    }

    @Test
    void userGenderTest() {
        ResourceBundle bundleEn = new MessagesBundle(new Locale("en", "US", ""));
        assertEquals("Unassigned", bundleEn.getString("user.gender.UNASSIGNED"));
        assertEquals("Male", bundleEn.getString("user.gender.MALE"));
        assertEquals("Female", bundleEn.getString("user.gender.FEMALE"));
        assertEquals("Other", bundleEn.getString("user.gender.OTHER"));

        ResourceBundle bundleDe = new MessagesBundle(new Locale("de"));
        assertEquals("Nicht ausgewählt", bundleDe.getString("user.gender.UNASSIGNED"));
        assertEquals("Männlich", bundleDe.getString("user.gender.MALE"));
        assertEquals("Weiblich", bundleDe.getString("user.gender.FEMALE"));
        assertEquals("Divers", bundleDe.getString("user.gender.OTHER"));
    }
}
