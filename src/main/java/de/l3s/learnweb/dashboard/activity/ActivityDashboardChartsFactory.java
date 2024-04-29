package de.l3s.learnweb.dashboard.activity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.l3s.util.ColorHelper;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.color.Color;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.options.elements.Fill;

public final class ActivityDashboardChartsFactory {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String createActivitiesChart(List<ActivityGraphData> data, LocalDate startDate, LocalDate endDate) {
        LineData chartData = new LineData();

        List<Color> colors = ColorHelper.getColorList(data.size());

        for (ActivityGraphData activityData : data) {
            List<BigDecimal> values = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
                String dateKey = DATE_FORMAT.format(date);
                labels.add(dateKey);
                values.add(BigDecimal.valueOf(activityData.getActionsPerDay().getOrDefault(dateKey, 0)));
            }

            chartData.addDataset(new LineDataset()
                .setData(values)
                .setFill(new Fill<>(false))
                .setBorderColor(colors.getFirst())
                .setLabel(activityData.getName())
                .setLineTension(0.1f));
            colors.removeFirst();
            chartData.setLabels(labels);
        }

        return new LineChart().setData(chartData).toJson();
    }

    public static List<Map<String, Object>> createActivitiesTable(List<ActivityGraphData> data, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            Map<String, Object> columns = new LinkedHashMap<>();
            String dateKey = DATE_FORMAT.format(date);
            columns.put("Date", dateKey);

            for (ActivityGraphData activityData : data) {
                columns.put(activityData.getName(), activityData.getActionsPerDay().getOrDefault(dateKey, 0));
            }
            rows.add(columns);
        }

        return rows;
    }

    public static class ActivityGraphData {
        private String name;

        private Map<String, Integer> actionsPerDay;

        public Map<String, Integer> getActionsPerDay() {
            return actionsPerDay;
        }

        public void setActionsPerDay(Map<String, Integer> actionsPerDay) {
            this.actionsPerDay = actionsPerDay;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
