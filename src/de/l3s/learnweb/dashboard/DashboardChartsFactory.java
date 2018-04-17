package de.l3s.learnweb.dashboard;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.util.MapHelper;
import org.primefaces.model.chart.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

class DashboardChartsFactory
{
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static BarChartModel createActivityTypesChart(final Map<Integer, Integer> actionsMap)
    {
        BarChartModel model = new BarChartModel();

        Action[] actionTypes = Action.values();

        int search = 0;
        int glossary = 0;
        int resource = 0;
        int system = 0;

        for(Integer actionId : actionsMap.keySet())
        {
            Action action = actionTypes[actionId];
            if(Action.getSearchActions().contains(action))
                search += actionsMap.get(actionId);
            else if(Action.getGlossaryActions().contains(action))
                glossary += actionsMap.get(actionId);
            else if(Action.getResourceActions().contains(action))
                resource += actionsMap.get(actionId);
            else
                system += actionsMap.get(actionId);
        }

        ChartSeries activity = new ChartSeries();
        activity.setLabel(UtilBean.getLocaleMessage("interactions"));
        activity.set(UtilBean.getLocaleMessage("glossary"), glossary);
        activity.set(UtilBean.getLocaleMessage("Search"), search);
        activity.set(UtilBean.getLocaleMessage("system"), system);
        activity.set(UtilBean.getLocaleMessage("resource"), resource);

        model.addSeries(activity);
        model.setLegendPosition("ne");

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);

        return model;
    }

    static PieChartModel createStudentsSourcesChart(Map<String, Integer> glossarySourcesWithCounters)
    {
        PieChartModel model = new PieChartModel();
        model.setDataFormat("percent");
        model.setShowDataLabels(true);
        model.setDataLabelThreshold(3);
        model.setLegendPosition("w");
        model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);

        if(glossarySourcesWithCounters.isEmpty())
        {
            model.set("", 0);
        }
        else
        {
            for(Map.Entry<String, Integer> source : glossarySourcesWithCounters.entrySet())
                model.set(source.getKey(), source.getValue());
        }

        return model;
    }

    static LineChartModel createInteractionsChart(Map<String, Integer> actionsCountPerDay, Date startDate, Date endDate)
    {
        LineChartModel model = new LineChartModel();

        LineChartSeries interactions = new LineChartSeries();
        interactions.setLabel("interactions");

        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        for(Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime())
        {
            String dateKey = dateFormat.format(date.toInstant().atZone(ZoneId.systemDefault()));
            interactions.set(dateKey, actionsCountPerDay.getOrDefault(dateKey, 0));
        }

        model.addSeries(interactions);
        model.setLegendPosition("e");
        model.setShowPointLabels(true);
        model.getAxes().put(AxisType.X, new CategoryAxis("Days"));
        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);
        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel("Interactions");
        yAxis.setMin(0);
        return model;
    }

    static BarChartModel createStudentsGlossaryChart(Map<String, Integer> glossaryConceptsCountPerUser, Map<String, Integer> glossaryTermsCountPerUser)
    {
        BarChartModel model = new BarChartModel();

        ChartSeries concepts = new ChartSeries();
        concepts.setLabel("Concepts");

        ChartSeries terms = new ChartSeries();
        terms.setLabel("Terms");

        if(glossaryConceptsCountPerUser.isEmpty())
        {
            concepts.set("", 0);
        }
        else
        {
            for(String key : glossaryConceptsCountPerUser.keySet())
            {
                concepts.set(key, glossaryConceptsCountPerUser.getOrDefault(key, 0));
            }
        }

        if(glossaryTermsCountPerUser.isEmpty())
        {
            terms.set("", 0);
        }
        else
        {
            for(String key : glossaryTermsCountPerUser.keySet())
            {
                terms.set(key, glossaryTermsCountPerUser.getOrDefault(key, 0));
            }
        }

        model.addSeries(concepts);
        model.addSeries(terms);
        model.setLegendPosition("ne");

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);

        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setMin(0);

        return model;
    }

    static BarChartModel createProxySourcesChart(Map<String, Integer> proxySourcesWithCounters)
    {
        BarChartModel model = new BarChartModel();
        BarChartSeries proxySource = new BarChartSeries();

        if(proxySourcesWithCounters.isEmpty())
        {
            proxySource.set("", 0);
        }
        else
        {
            for(Map.Entry<String, Integer> e : MapHelper.sortByValue(proxySourcesWithCounters).entrySet())
            {
                proxySource.set(e.getKey(), e.getValue());
            }
        }

        model.addSeries(proxySource);

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);

        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel("sources");

        return model;
    }

    static BarChartModel createStudentFieldsChart(List<DashboardManager.GlossaryFieldSummery> glossaryFieldSummeryPerUser)
    {
        BarChartModel model = new BarChartModel();
        ChartSeries activity = new ChartSeries();

        if(glossaryFieldSummeryPerUser.isEmpty())
        {
            activity.set("", 0);
        }
        else
        {
            DashboardManager.GlossaryFieldSummery gfs = glossaryFieldSummeryPerUser.get(0);
            activity.set("pronounciation", gfs.getPronounciation());
            activity.set("acronym", gfs.getAcronym());
            activity.set("phraseology", gfs.getPhraseology());
            activity.set("uses", gfs.getUses());
            activity.set("source", gfs.getSource());
        }

        model.addSeries(activity);
        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);
        return model;
    }
}
