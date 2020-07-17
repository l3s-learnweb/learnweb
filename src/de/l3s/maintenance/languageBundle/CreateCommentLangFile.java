package de.l3s.maintenance.languageBundle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This script creates a placeholder so that we can identify language entries that have missing translations.
 *
 * messages_xy.properties is used to store comments about translations
 * But most of the translations don't have any comments yet. Because of this all language entries are marked as incomplete.
 *
 * @author Philipp Kemkes
 */
public class CreateCommentLangFile {
    private static final Logger log = LogManager.getLogger(CreateCommentLangFile.class);

    public static void main(String[] args) throws IOException {
        ResourceBundle bundle = ResourceBundle.getBundle("de.l3s.learnweb.lang.messages");
        Enumeration<String> keys = bundle.getKeys();

        Properties comments = new Properties();
        comments.load(new FileInputStream("Resources/de/l3s/learnweb/lang/messages_xy.properties"));

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (comments.getProperty(key) == null) {
                comments.setProperty(key, "TODO");
                //log.debug(key);
            } else {
                log.debug(key);
            }
        }

        FileOutputStream out = new FileOutputStream("Resources/de/l3s/learnweb/lang/messages_xy.properties");
        comments.store(out, null);
        out.close();

    }

}
