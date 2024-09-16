package de.l3s.util;

import java.util.ArrayList;
import java.util.List;

import software.xdev.chartjs.model.color.RGBAColor;

/**
 * Provides colors that are applicable for the Learnweb theme.
 */
public final class ColorHelper {

    private static final List<RGBAColor> COLORS = List.of(
        new RGBAColor(0, 113, 188),
        new RGBAColor(106, 74, 165),
        new RGBAColor(159, 68, 136),
        new RGBAColor(254, 135, 93),
        new RGBAColor(249, 198, 66),
        new RGBAColor(74, 165, 100),
        new RGBAColor(7, 160, 137),
        new RGBAColor(0, 191, 231),
        new RGBAColor(75, 109, 126),
        new RGBAColor(96, 108, 136),
        new RGBAColor(81, 74, 157),
        new RGBAColor(104, 76, 78),
        new RGBAColor(133, 52, 78),
        new RGBAColor(74, 163, 130),
        new RGBAColor(91, 97, 107),
        new RGBAColor(81, 74, 157),
        new RGBAColor(143, 59, 79),
        new RGBAColor(75, 108, 183),
        new RGBAColor(49, 130, 123),
        new RGBAColor(74, 80, 93)
    );

    /**
     * Returns the color with the given index. If the index is greater than a color list, then the method will use the modulo if the given index.
     */
    public static RGBAColor getColor(int index) {
        return COLORS.get(Math.abs(index % COLORS.size()));
    }

    public static List<RGBAColor> getColorList(int size) {
        final ArrayList<RGBAColor> randColors = new ArrayList<>(COLORS);
        // Collections.shuffle(randColors);
        return fillColors(randColors, size);
    }

    @SuppressWarnings("CollectionAddedToSelf")
    private static List<RGBAColor> fillColors(ArrayList<RGBAColor> colors, int size) {
        if (colors.size() >= size) {
            return colors.subList(0, size);
        }

        colors.addAll(colors);
        return fillColors(colors, size);
    }
}
