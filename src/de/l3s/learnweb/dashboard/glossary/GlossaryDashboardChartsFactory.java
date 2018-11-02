package de.l3s.learnweb.dashboard.glossary;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.BarChartSeries;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LegendPlacement;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.PieChartModel;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.learnweb.user.User;
import de.l3s.util.MapHelper;
import de.l3s.util.StringHelper;

public class GlossaryDashboardChartsFactory
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

    public static PieChartModel createUsersSourcesChart(Map<String, Integer> glossarySourcesWithCounters)
    {
        PieChartModel model = new PieChartModel();
        model.setDataFormat("percent");
        model.setShowDataLabels(true);
        model.setDataLabelThreshold(3);
        model.setLegendPosition("s");
        model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
        model.setLegendCols(2);

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

    public static LineChartModel createInteractionsChart(Map<String, Integer> actionsCountPerDay, Date startDate, Date endDate)
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

    public static BarChartModel createUsersGlossaryChart(Map<String, Integer> glossaryConceptsCountPerUser, Map<String, Integer> glossaryTermsCountPerUser)
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
        model.setStacked(true);

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);

        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setMin(0);

        return model;
    }

    public static BarChartModel createProxySourcesChart(Map<String, Integer> proxySourcesWithCounters)
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

    public static BarChartModel createUserFieldsChart(List<GlossaryFieldSummery> glossaryFieldSummeryPerUser)
    {
        BarChartModel model = new BarChartModel();
        ChartSeries activity = new ChartSeries();

        if(glossaryFieldSummeryPerUser.isEmpty())
        {
            activity.set("", 0);
        }
        else
        {
            GlossaryFieldSummery gfs = glossaryFieldSummeryPerUser.get(0);
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

    public static class GlossaryStatistic implements Serializable
    {
        private static final long serialVersionUID = -4378112533840640208L;
        private int userId = -1;
        private int totalGlossaries = 0;
        private int totalTerms = 0;
        private int totalReferences = 0;

        private transient User user;

        public GlossaryStatistic()
        {
        }

        public GlossaryStatistic(int userId)
        {
            this.userId = userId;
        }

        public User getUser()
        {
            if(null == user && userId > 0)
            {
                try
                {
                    user = Learnweb.getInstance().getUserManager().getUser(userId);
                }
                catch(SQLException e)
                {
                    log.fatal("can't get user: " + userId, e);
                }
            }
            return user;
        }

        public int getUserId()
        {
            return userId;
        }

        public void setUserId(int userId)
        {
            this.userId = userId;
        }

        public int getTotalGlossaries()
        {
            return totalGlossaries;
        }

        public void setTotalGlossaries(int totalGlossaries)
        {
            this.totalGlossaries = totalGlossaries;
        }

        public int getTotalTerms()
        {
            return totalTerms;
        }

        public void setTotalTerms(int totalTerms)
        {
            this.totalTerms = totalTerms;
        }

        public int getTotalReferences()
        {
            return totalReferences;
        }

        public void setTotalReferences(int totalReferences)
        {
            this.totalReferences = totalReferences;
        }
    }

    public static class DescFieldData implements Serializable
    {
        private static final long serialVersionUID = -4378112533840640208L;
        Integer userId;
        String description;
        String lang;
        Integer length;
        Integer entryId;

        private transient User user;

        public DescFieldData()
        {
        }

        public User getUser()
        {
            if(null == user && userId > 0)
            {
                try
                {
                    user = Learnweb.getInstance().getUserManager().getUser(userId);
                }
                catch(SQLException e)
                {
                    log.fatal("can't get user: " + userId, e);
                }
            }
            return user;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
            if(description != null && !description.isEmpty())
                this.length = description.split(" ").length;
        }

        public String getLang()
        {
            return lang;
        }

        public void setLang(String lang)
        {
            this.lang = lang;
        }

        public Integer getLength()
        {
            return length;
        }

        public void setLength(Integer length)
        {
            this.length = length;
        }

        public Integer getUserId()
        {
            return userId;
        }

        public void setUserId(Integer userId)
        {
            this.userId = userId;
        }

        public Integer getEntryId()
        {
            return entryId;
        }

        public void setEntryId(Integer entryId)
        {
            this.entryId = entryId;
        }

    }

    public static class TrackerStatistic
    {
        private int userId;
        private int totalEvents;
        private int timeStay;
        private int timeActive;
        private int clicks;
        private int keyPresses;

        private long timeActiveInMinutes;
        private String timeActiveFormatted;
        private long timeStayInMinutes;
        private String timeStayFormatted;

        public int getUserId()
        {
            return userId;
        }

        public void setUserId(int userId)
        {
            this.userId = userId;
        }

        public int getTotalEvents()
        {
            return totalEvents;
        }

        public void setTotalEvents(int totalEvents)
        {
            this.totalEvents = totalEvents;
        }

        public int getTimeStay()
        {
            return timeStay;
        }

        public void setTimeStay(int timeStay)
        {
            this.timeStay = timeStay;

            Duration durationStay = Duration.ofMillis(timeStay);
            this.timeStayInMinutes = durationStay.toMinutes();
            this.timeStayFormatted = StringHelper.formatDuration(durationStay);
        }

        public int getTimeActive()
        {
            return timeActive;
        }

        public void setTimeActive(int timeActive)
        {
            this.timeActive = timeActive;

            Duration durationActive = Duration.ofMillis(timeActive);
            this.timeActiveInMinutes = durationActive.toMinutes();
            this.timeActiveFormatted = StringHelper.formatDuration(durationActive);
        }

        public int getClicks()
        {
            return clicks;
        }

        public void setClicks(int clicks)
        {
            this.clicks = clicks;
        }

        public int getKeyPresses()
        {
            return keyPresses;
        }

        public void setKeyPresses(int keyPresses)
        {
            this.keyPresses = keyPresses;
        }

        public long getTimeActiveInMinutes()
        {
            return timeActiveInMinutes;
        }

        public void setTimeActiveInMinutes(long timeActiveInMinutes)
        {
            this.timeActiveInMinutes = timeActiveInMinutes;
        }

        public String getTimeActiveFormatted()
        {
            return timeActiveFormatted;
        }

        public void setTimeActiveFormatted(String timeActiveFormatted)
        {
            this.timeActiveFormatted = timeActiveFormatted;
        }

        public long getTimeStayInMinutes()
        {
            return timeStayInMinutes;
        }

        public void setTimeStayInMinutes(long timeStayInMinutes)
        {
            this.timeStayInMinutes = timeStayInMinutes;
        }

        public String getTimeStayFormatted()
        {
            return timeStayFormatted;
        }

        public void setTimeStayFormatted(String timeStayFormatted)
        {
            this.timeStayFormatted = timeStayFormatted;
        }
    }

    public static class GlossaryFieldSummery implements Serializable
    {
        private static final long serialVersionUID = -4378112533840640208L;

        private int userId = -1;
        private int terms;
        private int termsPasted;
        private int pronounciation;
        private int pronounciationPasted;
        private int acronym;
        private int acronymPasted;
        private int phraseology;
        private int phraseologyPasted;
        private int uses;
        private int source;
        private int entries;

        private transient User user;

        public float getAvg()
        {
            return ((float) (pronounciation + acronym + phraseology + uses + source) / (terms * 5));
        }

        public float getTotalPastedPct()
        {
            long totalFields = pronounciation + acronym + phraseology + terms;
            long pastedFields = pronounciationPasted + acronymPasted + phraseologyPasted + termsPasted;
            return ((float) pastedFields / totalFields);
        }

        public User getUser()
        {
            if(null == user && userId > 0)
            {
                try
                {
                    user = Learnweb.getInstance().getUserManager().getUser(userId);
                }
                catch(SQLException e)
                {
                    log.fatal("can't get user: " + userId, e);
                }
            }
            return user;
        }

        public int getEntries()
        {
            return entries;
        }

        public void setEntries(int entries)
        {
            this.entries = entries;
        }

        public int getUserId()
        {
            return userId;
        }

        public void setUserId(final int userId)
        {
            this.userId = userId;
        }

        public int getTerms()
        {
            return terms;
        }

        public void setTerms(final int total)
        {
            this.terms = total;
        }

        public int getTermsPasted()
        {
            return termsPasted;
        }

        public void setTermsPasted(final int termsPasted)
        {
            this.termsPasted = termsPasted;
        }

        public int getPronounciation()
        {
            return pronounciation;
        }

        public void setPronounciation(final int pronounciation)
        {
            this.pronounciation = pronounciation;
        }

        public int getPronounciationPasted()
        {
            return pronounciationPasted;
        }

        public void setPronounciationPasted(final int pronounciationPasted)
        {
            this.pronounciationPasted = pronounciationPasted;
        }

        public int getAcronym()
        {
            return acronym;
        }

        public void setAcronym(final int acronym)
        {
            this.acronym = acronym;
        }

        public int getAcronymPasted()
        {
            return acronymPasted;
        }

        public void setAcronymPasted(final int acronymPasted)
        {
            this.acronymPasted = acronymPasted;
        }

        public int getPhraseology()
        {
            return phraseology;
        }

        public void setPhraseology(final int phraseology)
        {
            this.phraseology = phraseology;
        }

        public int getPhraseologyPasted()
        {
            return phraseologyPasted;
        }

        public void setPhraseologyPasted(final int phraseologyPasted)
        {
            this.phraseologyPasted = phraseologyPasted;
        }

        public int getUses()
        {
            return uses;
        }

        public void setUses(final int uses)
        {
            this.uses = uses;
        }

        public int getSource()
        {
            return source;
        }

        public void setSource(final int source)
        {
            this.source = source;
        }
    }
}
