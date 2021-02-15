package de.l3s.learnweb;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

class LanguageBundleTest {

    @Test
    void getString() {
        ResourceBundle bundleEn = new LanguageBundle(new Locale("en", "US", ""));
        assertEquals("Welcome to Learnweb", bundleEn.getString("homepageTitle"));

        // not existing language, fallback to default
        ResourceBundle bundleYY = new LanguageBundle(new Locale("yy"));
        assertEquals("Welcome to Learnweb", bundleYY.getString("homepageTitle"));

        ResourceBundle bundleDe = new LanguageBundle(new Locale("de"));
        assertEquals("Willkommen bei Learnweb", bundleDe.getString("homepageTitle"));

        ResourceBundle bundlePt = new LanguageBundle(new Locale("pt"));
        assertEquals("Bem-vindo ao Learnweb", bundlePt.getString("homepageTitle"));
    }
}
