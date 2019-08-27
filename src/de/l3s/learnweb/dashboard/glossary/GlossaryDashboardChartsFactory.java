package de.l3s.learnweb.dashboard.glossary;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import de.l3s.learnweb.dashboard.ChartJsUtils;
import org.apache.log4j.Logger;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.util.MapHelper;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.optionconfig.tooltip.Tooltip;
import org.primefaces.model.charts.pie.PieChartDataSet;
import org.primefaces.model.charts.pie.PieChartModel;

class GlossaryDashboardChartsFactory
{
    private static final Logger log = Logger.getLogger(GlossaryDashboardChartsFactory.class);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static BarChartModel createActivityTypesChart(final Map<Integer, Integer> actionsMap)
    {
        BarChartModel model = new BarChartModel();

        Action[] actionTypes = Action.values();

        int search = 0;
        int glossary = 0;
        int resource = 0;
        int system = 0;

        for(Integer actionId : actionsMap.keySet())
        {
            if(actionId < actionTypes.length)
            {
                Action action = actionTypes[actionId];
                if(Action.getActionsByCategory(ActionCategory.SEARCH).contains(action))
                    search += actionsMap.get(actionId);
                else if(Action.getActionsByCategory(ActionCategory.GLOSSARY).contains(action))
                    glossary += actionsMap.get(actionId);
                else if(Action.getActionsByCategory(ActionCategory.RESOURCE).contains(action))
                    resource += actionsMap.get(actionId);
                else
                    system += actionsMap.get(actionId);
            }
            else
            {
                log.error("Unknown actionId: " + actionId);
            }
        }

        ChartData data = new ChartData();
        List<String> labels = new ArrayList<>();
        labels.add(UtilBean.getLocaleMessage("glossary"));
        labels.add(UtilBean.getLocaleMessage("Search"));
        labels.add(UtilBean.getLocaleMessage("system"));
        labels.add(UtilBean.getLocaleMessage("resource"));
        data.setLabels(labels);

        BarChartDataSet barDataSet = new BarChartDataSet();
        barDataSet.setLabel(UtilBean.getLocaleMessage("interactions"));
        List<Number> values = new ArrayList<>();
        values.add(glossary);
        values.add(search);
        values.add(system);
        values.add(resource);
        barDataSet.setData(values);
        barDataSet.setBackgroundColor(ChartJsUtils.getColorList(4));
        data.addChartDataSet(barDataSet);

        model.setData(data);
        return model;
    }

    public static PieChartModel createUsersSourcesChart(Map<String, Integer> glossarySourcesWithCounters)
    {
        PieChartModel model = new PieChartModel();

        ChartData data = new ChartData();
        PieChartDataSet dataSet = new PieChartDataSet();

        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if(glossarySourcesWithCounters.isEmpty())
        {
            labels.add("");
            values.add(0);
        }
        else
        {
            for(Map.Entry<String, Integer> source : glossarySourcesWithCounters.entrySet()) {

                labels.add(source.getKey());
                values.add(source.getValue());
            }
        }

        dataSet.setData(values);
        data.setLabels(labels);
        data.addChartDataSet(dataSet);
        dataSet.setBackgroundColor(ChartJsUtils.getColorList(4));

        model.setData(data);
        return model;
    }

    public static LineChartModel createInteractionsChart(Map<String, Integer> actionsCountPerDay, Date startDate, Date endDate)
    {
        LineChartModel model = new LineChartModel();
        ChartData data = new ChartData();
        LineChartDataSet dataSet = new LineChartDataSet();

        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        dataSet.setData(values);
        dataSet.setFill(false);
        dataSet.setBorderColor("rgb(75, 192, 192)");
        dataSet.setLabel("interactions");
        dataSet.setLineTension(0.1);

        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        for(Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime())
        {
            String dateKey = dateFormat.format(date.toInstant().atZone(ZoneId.systemDefault()));
            labels.add(dateKey);
            values.add(actionsCountPerDay.getOrDefault(dateKey, 0));
        }

        data.addChartDataSet(dataSet);
        data.setLabels(labels);

        model.setData(data);
        return model;
    }

    public static BarChartModel createUsersGlossaryChart(Map<String, Integer> glossaryConceptsCountPerUser, Map<String, Integer> glossaryTermsCountPerUser)
    {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        List<String> labels = new ArrayList<>();

        BarChartDataSet concepts = new BarChartDataSet();
        concepts.setLabel("Concepts");

        List<Number> conceptsData = new ArrayList<>();
        if(glossaryConceptsCountPerUser.isEmpty())
        {
            conceptsData.add(0);
            labels.add("");
        }
        else
        {
            for(String key : glossaryConceptsCountPerUser.keySet())
            {
                labels.add(key);
                conceptsData.add(glossaryConceptsCountPerUser.getOrDefault(key, 0));
            }
        }
        concepts.setData(conceptsData);
        concepts.setBackgroundColor(ChartJsUtils.getColorList(10));

        BarChartDataSet terms = new BarChartDataSet();
        terms.setLabel("Terms");

        List<Number> termsData = new ArrayList<>();
        if(glossaryTermsCountPerUser.isEmpty())
        {
            conceptsData.add(0);
        }
        else
        {
            for(String key : glossaryTermsCountPerUser.keySet())
            {
                conceptsData.add(glossaryTermsCountPerUser.getOrDefault(key, 0));
            }
        }
        terms.setData(termsData);
        terms.setBackgroundColor(ChartJsUtils.getColorList(10));

        data.setLabels(labels);
        data.addChartDataSet(terms);
        data.addChartDataSet(concepts);

        model.setData(data);

        return model;
    }

    public static BarChartModel createProxySourcesChart(Map<String, Integer> proxySourcesWithCounters)
    {
        BarChartModel model = new BarChartModel();
        ChartData data = new ChartData();

        BarChartDataSet barDataSet = new BarChartDataSet();

        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if(proxySourcesWithCounters.isEmpty())
        {
            labels.add("");
            values.add(0);
        }
        else
        {
            for(Map.Entry<String, Integer> e : MapHelper.sortByValue(proxySourcesWithCounters).entrySet())
            {
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

    public static BarChartModel createUserFieldsChart(List<GlossaryUserTermsSummary> glossaryFieldSummeryPerUser)
    {
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

        if(!glossaryFieldSummeryPerUser.isEmpty())
        {
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
