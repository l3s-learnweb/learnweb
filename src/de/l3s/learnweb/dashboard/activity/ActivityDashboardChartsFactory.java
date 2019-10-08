package de.l3s.learnweb.dashboard.activity;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.LinkedMap;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;

import de.l3s.learnweb.beans.ColorUtils;

public class ActivityDashboardChartsFactory
{
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LineChartModel createActivitiesChart(List<ActivityGraphData> data, Date startDate, Date endDate)
    {
        LineChartModel model = new LineChartModel();
        ChartData chartData = new ChartData();
        List<String> colors = ColorUtils.getColorList(data.size());

        for(ActivityGraphData activityData : data)
        {
            LineChartDataSet dataSet = new LineChartDataSet();

            List<Number> values = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            dataSet.setData(values);
            dataSet.setFill(false);
            dataSet.setBorderColor(colors.get(0));
            colors.remove(0);
            dataSet.setLabel(activityData.getName());
            dataSet.setLineTension(0.1);

            Calendar start = Calendar.getInstance();
            start.setTime(startDate);
            Calendar end = Calendar.getInstance();
            end.setTime(endDate);

            for(Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime())
            {
                String dateKey = dateFormat.format(date.toInstant().atZone(ZoneId.systemDefault()));
                labels.add(dateKey);
                values.add(activityData.getActionsPerDay().getOrDefault(dateKey, 0));
            }

                chartData.addChartDataSet(dataSet);
                chartData.setLabels(labels);
        }

        model.setData(chartData);
        return model;
    }

    public static List<Map<String, Object>> createActivitiesTable(List<ActivityGraphData> data, Date startDate, Date endDate)
    {
        List<Map<String, Object>> rows = new ArrayList<>();

        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        for(Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime())
        {
            Map<String, Object> columns = new LinkedMap<>();
            String dateKey = dateFormat.format(date.toInstant().atZone(ZoneId.systemDefault()));
            columns.put("Date", dateKey);

            for(ActivityGraphData activityData : data)
            {
                columns.put(activityData.getName(), activityData.getActionsPerDay().getOrDefault(dateKey, 0));
            }
            rows.add(columns);
        }

        return rows;
    }

    public static class ActivityGraphData
    {
        private String name;

        private Map<String, Integer> actionsPerDay;

        public Map<String, Integer> getActionsPerDay()
        {
            return actionsPerDay;
        }

        public void setActionsPerDay(Map<String, Integer> actionsPerDay)
        {
            this.actionsPerDay = actionsPerDay;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }
}
