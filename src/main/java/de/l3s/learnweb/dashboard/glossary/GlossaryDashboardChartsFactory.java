package de.l3s.learnweb.dashboard.glossary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.pie.PieChartDataSet;
import org.primefaces.model.charts.pie.PieChartModel;

import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.util.ColorHelper;
import de.l3s.util.MapHelper;

final class GlossaryDashboardChartsFactory {
    private static final Logger log = LogManager.getLogger(GlossaryDashboardChartsFactory.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static BarChartModel createActivityTypesChart(final Map<Integer, Integer> actionsMap) {
        BarChartModel model = new BarChartModel();

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

        BarChartDataSet barDataSet = new BarChartDataSet();
        barDataSet.setLabel("Interactions");
        barDataSet.setData(Arrays.asList(glossary, search, system, resource));
        barDataSet.setBackgroundColor(ColorHelper.getColorList(4));

        ChartData data = new ChartData();
        data.setLabels(Arrays.asList("Glossary", "Search", "System", "Resources"));
        data.addChartDataSet(barDataSet);
        model.setData(data);
        return model;
    }

    public static PieChartModel createUsersSourcesChart(Map<String, Integer> glossarySourcesWithCounters) {
        PieChartModel model = new PieChartModel();

        ChartData data = new ChartData();
        PieChartDataSet dataSet = new PieChartDataSet();

        List<Number> values = new ArrayList<>();
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

        dataSet.setData(values);
        data.setLabels(labels);
        data.addChartDataSet(dataSet);
        dataSet.setBackgroundColor(ColorHelper.getColorList(20));

        model.setData(data);
        return model;
    }

    public static LineChartModel createInteractionsChart(Map<String, Integer> actionsCountPerDay, LocalDate startDate, LocalDate endDate) {
        LineChartModel model = new LineChartModel();
        ChartData data = new ChartData();
        LineChartDataSet dataSet = new LineChartDataSet();

        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        dataSet.setData(values);
        dataSet.setFill(false);
        dataSet.setBorderColor("rgb(75, 192, 192)");
        dataSet.setLabel("interactions");
        dataSet.setTension(0.1);

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            String dateKey = DATE_FORMAT.format(date);
            labels.add(dateKey);
            values.add(actionsCountPerDay.getOrDefault(dateKey, 0));
        }

        data.addChartDataSet(dataSet);
        data.setLabels(labels);

        model.setData(data);
        return model;
    }

    public static BarChartModel createUsersGlossaryChart(Map<String, Integer> glossaryConceptsCountPerUser, Map<String, Integer> glossaryTermsCountPerUser) {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        List<String> labels = new ArrayList<>();

        BarChartDataSet concepts = new BarChartDataSet();
        concepts.setLabel("Concepts");

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
        concepts.setData(conceptsData);
        concepts.setBackgroundColor(ColorHelper.getColorList(10));

        BarChartDataSet terms = new BarChartDataSet();
        terms.setLabel("Terms");

        List<Number> termsData = new ArrayList<>();
        if (glossaryTermsCountPerUser.isEmpty()) {
            conceptsData.add(0);
        } else {
            for (String key : glossaryTermsCountPerUser.keySet()) {
                conceptsData.add(glossaryTermsCountPerUser.getOrDefault(key, 0));
            }
        }
        terms.setData(termsData);
        terms.setBackgroundColor(ColorHelper.getColorList(10));

        data.setLabels(labels);
        data.addChartDataSet(terms);
        data.addChartDataSet(concepts);

        model.setData(data);

        return model;
    }

    public static BarChartModel createProxySourcesChart(Map<String, Integer> proxySourcesWithCounters) {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        BarChartDataSet barDataSet = new BarChartDataSet();

        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (proxySourcesWithCounters.isEmpty()) {
            labels.add("");
            values.add(0);
        } else {
            for (Map.Entry<String, Integer> e : MapHelper.sortByValue(proxySourcesWithCounters).entrySet()) {
                labels.add(e.getKey());
                values.add(e.getValue());
            }
        }

        barDataSet.setData(values);
        data.setLabels(labels);

        data.addChartDataSet(barDataSet);

        model.setData(data);
        return model;
    }

    public static BarChartModel createUserFieldsChart(List<GlossaryUserTermsSummary> glossaryFieldSummeryPerUser) {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        BarChartDataSet pronounciation = new BarChartDataSet();
        pronounciation.setLabel("pronounciation");

        BarChartDataSet acronym = new BarChartDataSet();
        acronym.setLabel("acronym");

        BarChartDataSet phraseology = new BarChartDataSet();
        phraseology.setLabel("phraseology");

        BarChartDataSet uses = new BarChartDataSet();
        uses.setLabel("uses");

        BarChartDataSet source = new BarChartDataSet();
        source.setLabel("source");

        if (!glossaryFieldSummeryPerUser.isEmpty()) {
            GlossaryUserTermsSummary gfs = glossaryFieldSummeryPerUser.get(0);
            pronounciation.setData(Collections.singletonList(gfs.getPronounciation()));
            acronym.setData(Collections.singletonList(gfs.getAcronym()));
            phraseology.setData(Collections.singletonList(gfs.getPhraseology()));
            uses.setData(Collections.singletonList(gfs.getUses()));
            source.setData(Collections.singletonList(gfs.getSource()));
        }

        data.addChartDataSet(pronounciation);
        data.addChartDataSet(acronym);
        data.addChartDataSet(phraseology);
        data.addChartDataSet(uses);
        data.addChartDataSet(source);
        model.setData(data);

        return model;
    }
}
