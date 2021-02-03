package de.l3s.maintenance.messages;

import java.io.FileOutputStream;
import java.util.Properties;

import de.l3s.maintenance.MaintenanceTask;
import de.l3s.util.MessagesHelper;

/**
 * This script creates a placeholder so that we can identify language entries that have missing translations.
 *
 * messages_xy.properties is used to store comments about translations
 * But most of the translations don't have any comments yet. Because of this all language entries are marked as incomplete.
 *
 * @author Philipp Kemkes
 */
public class CreateCommentLangFile extends MaintenanceTask {

    @Override
    protected void run(final boolean dryRun) throws Exception {
        Properties baseMessages = MessagesHelper.getMessagesForLocale(null);
        Properties xyMessages = MessagesHelper.getMessagesForLocale("xy");

        for (Object key : baseMessages.keySet()) {
            if (!xyMessages.containsKey(key)) {
                xyMessages.setProperty(String.valueOf(key), "TODO");
            }
        }

        try (FileOutputStream out = new FileOutputStream("Resources/" + MessagesHelper.MESSAGES_BUNDLE + "_xy.properties")) {
            xyMessages.store(out, null);
        }
    }

    public static void main(String[] args) {
        new CreateCommentLangFile().start(args);
    }
}
