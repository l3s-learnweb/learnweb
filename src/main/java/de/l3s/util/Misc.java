package de.l3s.util;

import java.util.Comparator;

import jakarta.faces.model.SelectItem;

public final class Misc {
    public static final Comparator<SelectItem> SELECT_ITEM_LABEL_COMPARATOR = Comparator.comparing(SelectItem::getLabel);

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
