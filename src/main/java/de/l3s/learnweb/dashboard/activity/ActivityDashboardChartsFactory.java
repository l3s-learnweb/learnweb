package de.l3s.learnweb.dashboard.activity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.LinkedMap;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;

import de.l3s.util.ColorHelper;

public final class ActivityDashboardChartsFactory {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LineChartModel createActivitiesChart(List<ActivityGraphData> data, LocalDate startDate, LocalDate endDate) {
        LineChartModel model = new LineChartModel();
        ChartData chartData = new ChartData();
        List<String> colors = ColorHelper.getColorList(data.size());

        for (ActivityGraphData activityData : data) {
            LineChartDataSet dataSet = new LineChartDataSet();

            List<Object> values = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            dataSet.setData(values);
            dataSet.setFill(false);
            dataSet.setBorderColor(colors.getFirst());
            colors.removeFirst();
            dataSet.setLabel(activityData.getName());
            dataSet.setTension(0.1);

            for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
                String dateKey = DATE_FORMAT.format(date);
                labels.add(dateKey);
                values.add(activityData.getActionsPerDay().getOrDefault(dateKey, 0));
            }

            chartData.addChartDataSet(dataSet);
            chartData.setLabels(labels);
        }

        model.setData(chartData);
        return model;
    }

    public static List<Map<String, Object>> createActivitiesTable(List<ActivityGraphData> data, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            Map<String, Object> columns = new LinkedMap<>();
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
