package jcdashboard.bean;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import jcdashboard.model.DescData;
import jcdashboard.model.TotalData;
import jcdashboard.model.UsesTable;
import jcdashboard.model.dao.UserLogHome;

import org.primefaces.model.chart.*;

@ManagedBean(name = "dashboard")
@ViewScoped
public class DashboardBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265538951073418345L;
    private static final Logger log = Logger.getLogger(DashboardBean.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private Course selectedCourse; // the visualized course
    private Date startDate = Date.from(ZonedDateTime.now().minusMonths(1).toInstant()); // month ago
    private Date endDate = new Date(); // now

    private UserLogHome ulh;

    private Integer totalconcepts = 0;
    private Integer totalterms = 0;
    private PieChartModel studentsSources;
    private BarChartModel studentsActivityTypes;
    private BarChartModel studentsGlossary;
    private LineChartModel interactionsChart;

    private ArrayList<TotalData> summarylist = null;
    private ArrayList<DescData> descdatalist = null;

    public DashboardBean()
    {
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        // for now just hard coded to use Francesca's course
        setSelectedCourse(getLearnweb().getCourseManager().getCourseById(1245));

        ulh = new UserLogHome();
    }

    public void onDateChanged()
    {
        studentsSources = null;
        studentsActivityTypes = null;
        interactionsChart = null;
        studentsGlossary = null;
    }

    public PieChartModel getStudentsSources()
    {
        if(studentsSources == null)
            studentsSources = initStudentsSources();
        return studentsSources;
    }

    public BarChartModel getStudentsActivityTypes()
    {
        if(studentsActivityTypes == null)
            studentsActivityTypes = initStudentsActivityTypes();
        return studentsActivityTypes;
    }

    public BarChartModel getStudentsGlossary()
    {
        if(studentsGlossary == null)
            studentsGlossary = initStudentsGlossary();
        return studentsGlossary;
    }

    public LineChartModel getInteractionsChart()
    {
        if(interactionsChart == null)
            interactionsChart = initInteractionsChart();
        return interactionsChart;
    }

    private BarChartModel initStudentsActivityTypes()
    {
        BarChartModel model = new BarChartModel();

        int search = 0;
        int glossary = 0;
        int resource = 0;
        int system = 0;
        Map<String, Integer> mappa = ulh.actionCount(this.getStartDateString(), this.getEndDateString());

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

    private BarChartModel initStudentsGlossary()
    {
        BarChartModel model = new BarChartModel();

        ChartSeries concepts = new ChartSeries();
        ChartSeries terms = new ChartSeries();

        Map<String, Integer> mappaConcepts = ulh.getUserGlossaryConceptCountByCourse(selectedCourse, this.getStartDateString(), this.getEndDateString());

        for(String k : mappaConcepts.keySet())
        {
            concepts.set(k, mappaConcepts.keySet().contains(k) ? mappaConcepts.get(k) : 0);
        }

        Map<String, Integer> mappaTerms = ulh.userGlossaryTerm(this.getStartDateString(), this.getEndDateString());

        for(String k : mappaTerms.keySet())
        {
            terms.set(k, mappaTerms.keySet().contains(k) ? mappaTerms.get(k) : 0);
        }

        concepts.setLabel("concepts");
        terms.setLabel("terms");

        model.addSeries(concepts);
        model.addSeries(terms);

        model.setLegendPosition("ne");
        Axis xAxis = model.getAxis(AxisType.X);
        xAxis.setTickAngle(-60);
        Axis yAxis = model.getAxis(AxisType.Y);
        yAxis.setMin(0);

        return model;
    }

    private PieChartModel initStudentsSources()
    {
        PieChartModel model = new PieChartModel();
        model.setDataFormat("percent");
        model.setShowDataLabels(true);
        model.setDataLabelThreshold(3);
        model.setLegendPosition("w");
        model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);

        model.set("glossary", 8);
        model.set("patients\\ websites and blogs", 48);
        model.set("encyclopaedia", 54);
        model.set("other", 86);
        model.set("scientific/academic publication", 127);
        model.set("Linguee or Reverso", 229);
        model.set("bilingual dictionary", 238);
        model.set("institutional website", 296);
        model.set("monolingual dictionary", 396);
        model.set("Wikipedia", 876);

        return model;
    }

    private LineChartModel initInteractionsChart()
    {
        LineChartModel model = new LineChartModel();

        Map<String, Integer> mappa = ulh.actionPerDay(this.getStartDateString(), this.getEndDateString());

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

    public ArrayList<UsesTable> getFields()
    {
        return ulh.fields();
    }

    public Integer getTotalconcepts()
    {
        if(totalconcepts == 0)
        {
            totalconcepts = ulh.getTotalConcepts(this.getStartDateString(), this.getEndDateString());
        }
        return totalconcepts;
    }

    public Integer getTotalterms()
    {
        if(totalterms == 0)
        {
            totalterms = ulh.getTotalTerms(this.getStartDateString(), this.getEndDateString());
        }
        return totalterms;
    }

    public ArrayList<TotalData> getSummary()
    {
        // System.out.println("CHAIMATO SUMMARY");
        if(summarylist == null)
        {
            summarylist = new ArrayList<TotalData>();
            // System.out.println("DENTRO SUMMARY");
            String[] userid = new String[]{"10413", "10430", "10429", "10443", "10411", "10152", "10111", "10117", "10428", "10113"};
            UserLogHome ulh = new UserLogHome();
            ;

            Map<String, Integer[]> summap = ulh.getSummary2(this.getStartDateString(), this.getEndDateString());

            for(String uid : userid)
            {
                TotalData td = new TotalData();
                td.setUserid(uid);
                if(summap.containsKey(uid))
                {
                    Integer[] vals = summap.get(uid);
                    td.setConcepts(vals[0]);
                    td.setTerms(vals[1]);
                    td.setSources(vals[2]);
                }
                else
                {
                    td.setConcepts(0);
                    td.setTerms(0);
                    td.setSources(0);
                }
                summarylist.add(td);
            }
            ulh.closeConnection();
        }
        return summarylist;
    }

    public ArrayList<DescData> getDesclist()
    {
        if(descdatalist == null)
        {
            descdatalist = new ArrayList<DescData>();
            DescData d = new DescData();
            d.setDescription("Vitamin");
            d.setEnrtyid(1322);
            d.setLang("en");
            d.setLenght(1);
            d.setUserid(10111);
            descdatalist.add(d);

            d = new DescData();
            d.setDescription("Il diabete ");
            d.setEnrtyid(1428);
            d.setLang("it");
            d.setLenght(2);
            d.setUserid(10111);
            descdatalist.add(d);

            d = new DescData();
            d.setDescription("Epidemiology");
            d.setEnrtyid(1113);
            d.setLang("en");
            d.setLenght(1);
            d.setUserid(10150);
            descdatalist.add(d);

            Integer userid = 10109;
            Integer lenght = 1;
            String lang = "unk";
            d = new DescData();
            d.setDescription("g");
            d.setEnrtyid(1320);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            descdatalist.add(d);

            d = new DescData();
            d.setDescription("v");
            d.setEnrtyid(1323);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            descdatalist.add(d);

            d = new DescData();
            d.setDescription("c");
            d.setEnrtyid(1324);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            descdatalist.add(d);

            d = new DescData();
            d.setDescription("g");
            d.setEnrtyid(1327);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            descdatalist.add(d);

            d = new DescData();
            d.setDescription("b");
            d.setEnrtyid(1328);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            descdatalist.add(d);

            d = new DescData();
            d.setDescription("h");
            d.setEnrtyid(1350);
            d.setLang(lang);
            d.setLenght(lenght);
            d.setUserid(userid);
            descdatalist.add(d);
        }
        return descdatalist;
    }

    public void setSelectedCourse(Course selectedCourse)
    {
        this.selectedCourse = selectedCourse;
    }

    public Course getSelectedCourse()
    {
        return selectedCourse;
    }
}
