package de.l3s.learnweb.dashboard;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ChartJsUtils
{
    private static ImmutableList<String> colors = ImmutableList.of(
            "#0071bc",
            "#6A4AA5",
            "#9f4488",
            "#fe875d"
    );

    public static String getRandColor()
    {
        return colors.get(ThreadLocalRandom.current().nextInt(colors.size()));
    }

    public static List<String> getColorList(int size)
    {
        final ArrayList<String> randColors = new ArrayList<>(colors);
        // Collections.shuffle(randColors);
        return fillColors(randColors, size);
    }

    private static List<String> fillColors(ArrayList<String> colors, int size)
    {
        if (colors.size() >= size) {
            return colors.subList(0, size);
        }

        colors.addAll(colors);
        return fillColors(colors, size);
    }
}
