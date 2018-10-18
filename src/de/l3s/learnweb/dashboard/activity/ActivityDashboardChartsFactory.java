package de.l3s.learnweb.dashboard.activity;

import org.primefaces.model.chart.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ActivityDashboardChartsFactory
{
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LineChartModel createActivitiesChart(List<ActivityGraphData> data, Date startDate, Date endDate)
    {
        LineChartModel model = new LineChartModel();

        for(ActivityGraphData activityData : data)
        {
            LineChartSeries interactions = new LineChartSeries();
            interactions.setLabel(activityData.getName());

            Calendar start = Calendar.getInstance();
            start.setTime(startDate);
            Calendar end = Calendar.getInstance();
            end.setTime(endDate);

            for(Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime())
            {
                String dateKey = dateFormat.format(date.toInstant().atZone(ZoneId.systemDefault()));
                interactions.set(dateKey, activityData.getActionsPerDay().getOrDefault(dateKey, 0));
            }
            model.addSeries(interactions);
        }
        model.setLegendPosition("e");
        model.setShowPointLabels(true);
        model.getAxes().put(AxisType.X, new CategoryAxis("Days"));
        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);
        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel("Activities");
        yAxis.setMin(0);
        return model;
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
