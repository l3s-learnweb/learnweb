package de.l3s.util;

import java.util.ArrayList;
import java.util.List;

import software.xdev.chartjs.model.color.Color;

/**
 * Provides colors that are applicable for the Learnweb theme.
 */
public final class ColorHelper {

    private static final List<Color> COLORS = List.of(
        new Color(0, 113, 188),
        new Color(106, 74, 165),
        new Color(159, 68, 136),
        new Color(254, 135, 93),
        new Color(249, 198, 66),
        new Color(74, 165, 100),
        new Color(7, 160, 137),
        new Color(0, 191, 231),
        new Color(75, 109, 126),
        new Color(96, 108, 136),
        new Color(81, 74, 157),
        new Color(104, 76, 78),
        new Color(133, 52, 78),
        new Color(74, 163, 130),
        new Color(91, 97, 107),
        new Color(81, 74, 157),
        new Color(143, 59, 79),
        new Color(75, 108, 183),
        new Color(49, 130, 123),
        new Color(74, 80, 93)
    );

    /**
     * Returns the color with the given index. If the index is greater than color list then the method will use the modulo if the given index.
     */
    public static Color getColor(int index) {
        return COLORS.get(Math.abs(index % COLORS.size()));
    }

    public static List<Color> getColorList(int size) {
        final ArrayList<Color> randColors = new ArrayList<>(COLORS);
        // Collections.shuffle(randColors);
        return fillColors(randColors, size);
    }

    @SuppressWarnings("CollectionAddedToSelf")
    private static List<Color> fillColors(ArrayList<Color> colors, int size) {
        if (colors.size() >= size) {
            return colors.subList(0, size);
        }

        colors.addAll(colors);
        return fillColors(colors, size);
    }
}
