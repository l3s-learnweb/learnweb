package de.l3s.learnweb.dashboard.glossary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.util.ColorHelper;
import de.l3s.util.MapHelper;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.charts.PieChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.data.PieData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.dataset.PieDataset;

final class GlossaryDashboardChartsFactory {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String createActivityTypesChart(final Map<Integer, Integer> actionsMap, ResourceBundle bundle) {
        int search = 0;
        int glossary = 0;
        int resource = 0;
        int system = 0;

        for (final Map.Entry<Integer, Integer> entry : actionsMap.entrySet()) {
            // retired actions are counted as system actions
            ActionCategory category = Action.findById(entry.getKey()).map(Action::getCategory).orElse(ActionCategory.OTHER);
            switch (category) {
                case SEARCH -> search += entry.getValue();
                case GLOSSARY -> glossary += entry.getValue();
                case RESOURCE -> resource += entry.getValue();
                default -> system += entry.getValue();
            }
        }

        return new BarChart()
            .setData(new BarData()
                .setLabels(Arrays.asList(bundle.getString("activity.actions_glossary"), bundle.getString("activity.actions_search"),
                    bundle.getString("activity.actions_other"), bundle.getString("activity.actions_resource")))
                .addDataset(new BarDataset()
                    .setLabel(bundle.getString("interactions"))
                    .setData(glossary, search, system, resource)
                    .setBackgroundColor(ColorHelper.getColorList(4))))
            .toJson();
    }

    public static String createUsersSourcesChart(Map<String, Integer> sourceUsages) {
        List<Integer> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (sourceUsages.isEmpty()) {
            labels.add("");
            values.add(0);
        } else {
            for (Map.Entry<String, Integer> source : sourceUsages.entrySet()) {
                labels.add(source.getKey());
                values.add(source.getValue());
            }
        }

        return new PieChart()
            .setData(new PieData()
                .setLabels(labels)
                .addDataset(new PieDataset()
                    .setDataUnchecked(values)
                    .setBackgroundColor(ColorHelper.getColorList(20))))
            .toJson();
    }

    public static String createInteractionsChart(Map<String, Integer> actionsCountPerDay, LocalDate startDate, LocalDate endDate, ResourceBundle bundle) {
        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            String dateKey = DATE_FORMAT.format(date);
            labels.add(dateKey);
            values.add(actionsCountPerDay.getOrDefault(dateKey, 0));
        }

        return new LineChart()
            .setData(new LineData()
                .setLabels(labels)
                .addDataset(new LineDataset()
                    .setLabel(bundle.getString("interactions"))
                    .setLineTension(0.1f)
                    .setFill(false)
                    .setData(values)
                    .setBackgroundColor(new RGBAColor(75, 192, 192))))
            .toJson();
    }

    public static String createUsersGlossaryChart(Map<String, Integer> glossaryConceptsCountPerUser, Map<String, Integer> glossaryTermsCountPerUser,
        ResourceBundle bundle) {
        List<String> labels = new ArrayList<>();

        List<Number> conceptsData = new ArrayList<>();
        if (glossaryConceptsCountPerUser.isEmpty()) {
            conceptsData.add(0);
            labels.add("");
        } else {
            for (String key : glossaryConceptsCountPerUser.keySet()) {
                labels.add(key);
                conceptsData.add(glossaryConceptsCountPerUser.getOrDefault(key, 0));
            }
        }

        List<Number> termsData = new ArrayList<>();
        if (glossaryTermsCountPerUser.isEmpty()) {
            termsData.add(0);
        } else {
            for (String key : glossaryTermsCountPerUser.keySet()) {
                termsData.add(glossaryTermsCountPerUser.getOrDefault(key, 0));
            }
        }

        return new BarChart()
            .setData(new BarData()
                .setLabels(labels)
                .addDataset(new BarDataset()
                    .setLabel(bundle.getString("glossary.concepts"))
                    .setData(conceptsData)
                    .setBackgroundColor(ColorHelper.getColorList(10)))
                .addDataset(new BarDataset()
                    .setLabel(bundle.getString("glossary.terms"))
                    .setData(termsData)
                    .setBackgroundColor(ColorHelper.getColorList(10))))
            .toJson();
    }

    public static String createProxySourcesChart(Map<String, Integer> proxySourcesWithCounters) {
        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();

        if (proxySourcesWithCounters.isEmpty()) {
            labels.add("");
            values.add(0);
        } else {
            for (Map.Entry<String, Integer> e : MapHelper.sortByValue(proxySourcesWithCounters).entrySet()) {
                labels.add(e.getKey());
                values.add(e.getValue());
            }
        }

        return new BarChart()
            .setData(new BarData()
                .setLabels(labels)
                .addDataset(new BarDataset().setData(values)))
            .toJson();
    }
}
