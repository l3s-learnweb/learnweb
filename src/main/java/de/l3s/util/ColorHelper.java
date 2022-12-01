package de.l3s.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides colors that are applicable for the Learnweb theme.
 */
public final class ColorHelper {

    private static final List<String> COLORS = List.of(
        "#0071bc",
        "#6A4AA5",
        "#9f4488",
        "#fe875d",
        "#F9C642",
        "#4aa564",
        "#07A089",
        "#00bfe7",
        "#4B6D7E",
        "#606c88",
        "#514A9D",
        "#684c4e",
        "#85344E",
        "#4AA382",
        "#5B616B",
        "#514A9D",
        "#8f3b4f",
        "#4b6cb7",
        "#31827b",
        "#4a505d"
    );

    /**
     * Returns the color with the given index. If the index is greater than color list then the method will use the modulo if the given index.
     */
    public static String getColor(int index) {
        return COLORS.get(Math.abs(index % COLORS.size()));
    }

    public static List<String> getColorList(int size) {
        final ArrayList<String> randColors = new ArrayList<>(COLORS);
        // Collections.shuffle(randColors);
        return fillColors(randColors, size);
    }

    @SuppressWarnings("CollectionAddedToSelf")
    private static List<String> fillColors(ArrayList<String> colors, int size) {
        if (colors.size() >= size) {
            return colors.subList(0, size);
        }

        colors.addAll(colors);
        return fillColors(colors, size);
    }
}
