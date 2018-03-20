package jcdashboard.bean;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.User;
import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import jcdashboard.model.Description;
import jcdashboard.model.UsesTable;
import jcdashboard.model.dao.UserLogHome;
import org.primefaces.model.chart.*;

@ManagedBean(name = "studentlog")
@ViewScoped
public class StudentlogBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 3461152818600391381L;
    private static final Logger log = Logger.getLogger(StudentlogBean.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private Integer sid = -1;
    private Date startDate = Date.from(ZonedDateTime.now().minusMonths(1).toInstant()); // month ago
    private Date endDate = new Date(); // now

    UserLogHome ulh;

    private Integer totalconcepts = 0;
    private Integer totalterms = 0;
    private PieChartModel studentSources;
    private BarChartModel studentActivityTypes;
    private BarChartModel studentFields;
    private BarChartModel proxySources;
    private LineChartModel interactionsChart;

    private List<Description> dlist = null;

    public StudentlogBean()
    {
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        ulh = new UserLogHome();
    }

    public Integer getSid()
    {
        return sid;
    }

    public List<Entry<String, Integer>> getProxyloglist()
    {
        return entriesSortedByValues(ulh.proxySources(sid, this.getStartDateString(), this.getEndDateString()));
    }

    public void onDateChanged()
    {
        studentSources = null;
        studentActivityTypes = null;
        studentFields = null;
        proxySources = null;
        interactionsChart = null;
    }

    public PieChartModel getStudentSources()
    {
        if(studentSources == null)
            studentSources = initStudentSources();
        return studentSources;
    }

    public BarChartModel getStudentActivityTypes()
    {
        if(studentActivityTypes == null)
            studentActivityTypes = initStudentActivityTypes();
        return studentActivityTypes;
    }

    public BarChartModel getStudentFields()
    {
        if(studentFields == null)
            studentFields = initStudentFields();
        return studentFields;
    }

    public BarChartModel getProxySources()
    {
        if(proxySources == null)
            proxySources = initProxySources();
        return proxySources;
    }

    public LineChartModel getInteractionsChart()
    {
        if(interactionsChart == null)
            interactionsChart = initInteractionsChart();
        return interactionsChart;
    }

    private BarChartModel initStudentActivityTypes()
    {
        BarChartModel model = new BarChartModel();

        int search = 0;
        int glossary = 0;
        int resource = 0;
        int system = 0;
        Map<String, Integer> mappa = ulh.actionCount(this.sid, this.getStartDateString(), this.getEndDateString());

        for(String k : mappa.keySet())
        {
            if(k.contains("search"))
                search += mappa.get(k);
            else if(k.contains("glossary"))
                glossary += mappa.get(k);
            else if(k.contains("resource"))
                resource += mappa.get(k);
            else
                system += mappa.get(k);
        }

        ChartSeries activity = new ChartSeries();
        activity.setLabel("interactions");
        activity.set("Glossary", glossary);
        activity.set("Search", search);
        activity.set("System (login/logout)", system);
        activity.set("Resource", resource);

        model.addSeries(activity);
        model.setLegendPosition("ne");

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);

        return model;
    }

    private BarChartModel initStudentFields()
    {
        BarChartModel model = new BarChartModel();

        UserLogHome ulh = new UserLogHome();
        UsesTable ut = ulh.fields(sid, this.getStartDateString(), this.getEndDateString());
        ChartSeries activity = new ChartSeries();
        activity.set("pronounciation", ut.getPronounciation());
        activity.set("acronym", ut.getAcronym());
        activity.set("phraseology", ut.getPhraseology());
        activity.set("uses", ut.getUses());
        activity.set("source", ut.getSource());

        model.addSeries(activity);

        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);

        return model;
    }

    private BarChartModel initProxySources()
    {
        BarChartModel model = new BarChartModel();
        BarChartSeries proxySource = new BarChartSeries();

        List<Entry<String, Integer>> mappa = entriesSortedByValues(ulh.proxySources(sid, this.getStartDateString(), this.getEndDateString()));
        for(Entry<String, Integer> e : mappa)
        {
            proxySource.set(e.getKey(), e.getValue());
        }

        // if no data
        if (proxySource.getData().isEmpty()) {
            proxySource.set("1", 0);
        }

        model.addSeries(proxySource);
        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);

        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setLabel("sources");

        return model;
    }

    private LineChartModel initInteractionsChart()
    {
        LineChartModel model = new LineChartModel();

        Map<String, Integer> mappa = ulh.actionPerDay(this.sid, this.getStartDateString(), this.getEndDateString());

        LineChartSeries interactions = new LineChartSeries();
        interactions.setLabel("interactions");

        Calendar start = Calendar.getInstance();
        start.setTime(this.startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(this.endDate);

        for(Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime())
        {
            String dateKey = dateFormat.format(date);
            interactions.set(dateKey, mappa.keySet().contains(dateKey) ? mappa.get(dateKey) : 0);
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

    private PieChartModel initStudentSources()
    {
        PieChartModel model = new PieChartModel();
        model.setDataFormat("percent");
        model.setShowDataLabels(true);
        model.setDataLabelThreshold(3);
        model.setLegendPosition("w");
        model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);

        model.set("monolingual dictionary", 106);
        model.set("bilingual dictionary", 1);
        model.set("Empty", 17);

        return model;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public String getStartDateString()
    {
        return dateFormat.format(startDate);
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public String getEndDateString()
    {
        return dateFormat.format(endDate);
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    static <K, V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K, V> map)
    {
        List<Entry<K, V>> sortedEntries = new ArrayList<>(map.entrySet());
        Collections.sort(sortedEntries, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        return sortedEntries;
    }

    public void setSid(Integer sid)
    {
        this.sid = sid;
    }

    public List<Description> getDescriptions()
    {
        if(dlist == null)
        {
            UserLogHome ulh = new UserLogHome();
            dlist = new ArrayList<Description>();
            List<String> descriptions = ulh.descritpions(this.sid, this.getStartDateString(), this.getEndDateString());
            for(String description : descriptions)
            {
                Description d = new Description();
                String[] wordArray = description.trim().split("\\s+");
                d.setDescription(description);
                d.setLenght(wordArray.length);
                dlist.add(d);
            }
        }
        return dlist;
    }

    public Integer getTotalconcepts()
    {
        totalconcepts = ulh.getTotalConcepts(this.sid, this.getStartDateString(), this.getEndDateString());
        return totalconcepts;
    }

    public Integer getTotalterms()
    {
        totalterms = ulh.getTotalTerms(this.sid, this.getStartDateString(), this.getEndDateString());
        return totalterms;
    }

    public Integer getTotalsource()
    {
        return ulh.getTotalSource(this.sid, this.getStartDateString(), this.getEndDateString());
    }

    public String getRatiotc()
    {
        float res = (float) totalterms / totalconcepts;
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return twoDForm.format(res);
    }
}
