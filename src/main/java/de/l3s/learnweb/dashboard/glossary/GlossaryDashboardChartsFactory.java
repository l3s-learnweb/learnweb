package de.l3s.learnweb.dashboard.glossary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.util.ColorHelper;
import de.l3s.util.MapHelper;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.charts.PieChart;
import software.xdev.chartjs.model.color.Color;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.data.PieData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.dataset.PieDataset;
import software.xdev.chartjs.model.options.elements.Fill;

final class GlossaryDashboardChartsFactory {
    private static final Logger log = LogManager.getLogger(GlossaryDashboardChartsFactory.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String createActivityTypesChart(final Map<Integer, Integer> actionsMap) {
        Action[] actionTypes = Action.values();

        int search = 0;
        int glossary = 0;
        int resource = 0;
        int system = 0;

        for (final Map.Entry<Integer, Integer> entry : actionsMap.entrySet()) {
            Integer actionId = entry.getKey();
            if (actionId < actionTypes.length) {
                Action action = actionTypes[actionId];
                if (Action.getActionsByCategory(ActionCategory.SEARCH).contains(action)) {
                    search += entry.getValue();
                } else if (Action.getActionsByCategory(ActionCategory.GLOSSARY).contains(action)) {
                    glossary += entry.getValue();
                } else if (Action.getActionsByCategory(ActionCategory.RESOURCE).contains(action)) {
                    resource += entry.getValue();
                } else {
                    system += entry.getValue();
                }
            } else {
                log.error("Unknown actionId: {}", actionId);
            }
        }

        return new BarChart()
            .setData(new BarData()
                .setLabels(Arrays.asList("Glossary", "Search", "System", "Resources"))
                .addDataset(new BarDataset()
                    .setLabel("Interactions")
                    .setData(glossary, search, system, resource)
                    .setBackgroundColor(ColorHelper.getColorList(4))))
            .toJson();
    }

    public static String createUsersSourcesChart(Map<String, Integer> glossarySourcesWithCounters) {
        List<Integer> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (glossarySourcesWithCounters.isEmpty()) {
            labels.add("");
            values.add(0);
        } else {
            for (Map.Entry<String, Integer> source : glossarySourcesWithCounters.entrySet()) {

                labels.add(source.getKey());
                values.add(source.getValue());
            }
        }

        return new FixedPieChart()
            .setData(new PieData()
                .setLabels(labels)
                .addDataset(new PieDataset()
                    .setDataUnchecked(values)
                    .setBackgroundColor(ColorHelper.getColorList(20))))
            .toJson();
    }

    private static final class FixedPieChart extends PieChart {
        @Override
        public boolean isDrawable() {
            return true;
        }
    }

    public static String createInteractionsChart(Map<String, Integer> actionsCountPerDay, LocalDate startDate, LocalDate endDate) {
        List<BigDecimal> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            String dateKey = DATE_FORMAT.format(date);
            labels.add(dateKey);
            values.add(BigDecimal.valueOf(actionsCountPerDay.getOrDefault(dateKey, 0)));
        }

        return new LineChart()
            .setData(new LineData()
                .setLabels(labels)
                .addDataset(new LineDataset()
                    .setLabel("Interactions")
                    .setLineTension(0.1f)
                    .setFill(new Fill<>(false))
                    .setData(values)
                    .setBackgroundColor(new Color(75, 192, 192))))
            .toJson();
    }

    public static String createUsersGlossaryChart(Map<String, Integer> glossaryConceptsCountPerUser, Map<String, Integer> glossaryTermsCountPerUser) {
        List<String> labels = new ArrayList<>();

        List<BigDecimal> conceptsData = new ArrayList<>();
        if (glossaryConceptsCountPerUser.isEmpty()) {
            conceptsData.add(BigDecimal.ZERO);
            labels.add("");
        } else {
            for (String key : glossaryConceptsCountPerUser.keySet()) {
                labels.add(key);
                conceptsData.add(BigDecimal.valueOf(glossaryConceptsCountPerUser.getOrDefault(key, 0)));
            }
        }

        List<BigDecimal> termsData = new ArrayList<>();
        if (glossaryTermsCountPerUser.isEmpty()) {
            termsData.add(BigDecimal.ZERO);
        } else {
            for (String key : glossaryTermsCountPerUser.keySet()) {
                termsData.add(BigDecimal.valueOf(glossaryTermsCountPerUser.getOrDefault(key, 0)));
            }
        }

        return new BarChart()
            .setData(new BarData()
                .setLabels(labels)
                .addDataset(new BarDataset()
                    .setLabel("Concepts")
                    .setData(conceptsData)
                    .setBackgroundColor(ColorHelper.getColorList(10)))
                .addDataset(new BarDataset()
                    .setLabel("Terms")
                    .setData(termsData)
                    .setBackgroundColor(ColorHelper.getColorList(10))))
            .toJson();
    }

    public static String createProxySourcesChart(Map<String, Integer> proxySourcesWithCounters) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        if (proxySourcesWithCounters.isEmpty()) {
            labels.add("");
            values.add(BigDecimal.ZERO);
        } else {
            for (Map.Entry<String, Integer> e : MapHelper.sortByValue(proxySourcesWithCounters).entrySet()) {
                labels.add(e.getKey());
                values.add(BigDecimal.valueOf(e.getValue()));
            }
        }

        return new BarChart()
            .setData(new BarData()
                .setLabels(labels)
                .addDataset(new BarDataset().setData(values)))
            .toJson();
    }

    public static String createUserFieldsChart(List<GlossaryUserTermsSummary> summary) {
        GlossaryUserTermsSummary gfs = summary.getFirst();

        return new BarChart()
            .setData(new BarData()
                .addDataset(new BarDataset().setLabel("Pronounciation").setData(gfs.getPronounciation()))
                .addDataset(new BarDataset().setLabel("Acronym").setData(gfs.getAcronym()))
                .addDataset(new BarDataset().setLabel("Phraseology").setData(gfs.getPhraseology()))
                .addDataset(new BarDataset().setLabel("Uses").setData(gfs.getUses()))
                .addDataset(new BarDataset().setLabel("Source").setData(gfs.getSource()))
            ).toJson();
    }
}
