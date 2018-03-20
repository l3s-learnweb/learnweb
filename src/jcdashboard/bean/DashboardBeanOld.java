package jcdashboard.bean;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import jcdashboard.model.DescData;
import jcdashboard.model.Fields;
import jcdashboard.model.TotalData;
import jcdashboard.model.UsesTable;
import jcdashboard.model.dao.UserLogHome;

@ManagedBean
@ViewScoped
public class DashboardBeanOld extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265538951073418345L;

    private static final Logger log = Logger.getLogger(DashboardBeanOld.class);

    private Fields graph01 = null;
    private Fields graph02 = null;
    private String graph02color = "";

    private Fields graph03 = null;
    private String graph03terms = "";

    private String topbar01data = "";
    private boolean viewpanel1 = true;
    private Integer totalconcepts = 0;
    private Integer totalterms = 0;

    //private List<String> userlist;

    private Integer sid = 10410;

    private String startdate = "2017-03-01"; // "2017-03-02";
    private String enddate = "2017-06-01"; // "2017-04-02" new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    Map<String, String> graph02map = new HashMap<String, String>();

    ArrayList<TotalData> summarylist = null;
    ArrayList<DescData> descdatalist = null;

    private Course selectedCourse; // the visualized course

    private UserLogHome ulh;

    private Collection<HashMap<String, Object>> trackerStatistic;

    public DashboardBeanOld()
    {
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        // for now just hard coded to use Francesca's course
        setSelectedCourse(getLearnweb().getCourseManager().getCourseById(1245));

        ulh = new UserLogHome();

        /* graph02map.put("EMPTY", "rgba(38, 185, 154, 0.1)");
        graph02map.put("other", "rgb(102,205,170)");
        graph02map.put("glossary", "rgba(38, 185, 154, 0.2)");

        graph02map.put("Wikipedia", "rgba(3, 88, 120, 0.4)");
        graph02map.put("encyclopaedia", "rgba(3, 88, 120, 0.6)");

        graph02map.put("monolingual dictionary", "rgba(38, 175, 154, 0.3)");
        graph02map.put("bilingual dictionary", "rgba(38, 175, 154, 0.6)");
        graph02map.put("Linguee or Reverso", "rgba(38, 175, 154, 0.9)");

        graph02map.put("institutional website", "rgba(38, 105, 154, 0.3)");
        graph02map.put("patients' websites and blogs", "rgba(38, 105, 154, 0.6)");
        graph02map.put("scientific/academic publication", "rgba(38, 105, 154, 0.9)"); */

        graph02map.put("EMPTY", "#eff4fd");
        graph02map.put("other", "#5077bd");
        graph02map.put("glossary", "#324a76");

        graph02map.put("Wikipedia", "#e0e9fb");
        graph02map.put("encyclopaedia", "#d0dff9");

        graph02map.put("monolingual dictionary", "#c1d4f7");
        graph02map.put("bilingual dictionary", "#b1caf6");
        graph02map.put("Linguee or Reverso", "#a2bff4");

        graph02map.put("institutional website", "#92b4f2");
        graph02map.put("patients' websites and blogs", "#83aaf0");
        graph02map.put("scientific/academic publication", "#739fee");

    }

    public void updateCharts()
    {
        graph01 = null;
        graph02 = null;
        topbar01data = "";
        totalconcepts = 0;
        totalterms = 0;
        graph02color = "";
        graph03 = null;
        graph03terms = "";
        summarylist = null;
        descdatalist = null;
        trackerStatistic = null;
    }

    public String getStartdate()
    {
        return startdate;
    }

    public void setStartdate(String startdate)
    {
        this.startdate = startdate;
    }

    public String getEnddate()
    {
        return enddate;
    }

    public void setEnddate(String enddate)
    {
        this.enddate = enddate;
    }

    public Integer getSid()
    {
        return sid;
    }

    public Fields getGraph03()
    {
        if(graph03 == null)
        {
            Map<String, Integer> mappa = ulh.getUserGlossaryConceptCountByCourse(selectedCourse, this.startdate, this.enddate);

            String graph03label = " [ ";
            String graph03data = " [ ";
            for(String k : mappa.keySet())
            {
                graph03label += " \"" + k + "\", ";
                graph03data += " " + mappa.get(k) + ", ";
            }
            graph03label += " 		]";
            graph03data += " 		]";

            graph03 = new Fields();
            graph03.setLabel(graph03label);
            graph03.setData(graph03data);
        }
        return graph03;

    }

    public ArrayList<UsesTable> getFields()
    {
        return ulh.fields();
    }

    public String getGraph03terms()
    {
        if(graph03terms.compareTo("") == 0)
        {
            UserLogHome ulh = new UserLogHome();
            Map<String, Integer> mappa = ulh.userGlossaryTerm(this.startdate, this.enddate);
            graph03terms = " [ ";
            for(String k : mappa.keySet())
            {
                graph03terms += " " + mappa.get(k) + ", ";
            }
            graph03terms += " 		]";
        }
        return graph03terms;
    }

    public Integer getTotalconcepts()
    {
        if(totalconcepts == 0)
        {
            UserLogHome ulh = new UserLogHome();
            totalconcepts = ulh.getTotalConcepts(this.startdate, this.enddate);
        }
        return totalconcepts;
    }

    public Integer getTotalterms()
    {
        if(totalterms == 0)
        {
            UserLogHome ulh = new UserLogHome();
            totalterms = ulh.getTotalTerms(this.startdate, this.enddate);
        }
        return totalterms;
    }

    /*
    public Fields getGraph02()
    {
        if(graph02 == null)
        {
            String[] sourcelist = new String[] { "EMPTY", "Wikipedia", "encyclopaedia", "monolingual dictionary", "bilingual dictionary", "Linguee or Reverso", "institutional website", "patients' websites and blogs", "scientific/academic publication", "glossary", "other" };
            graph02 = new Fields();
            UserLogHome ulh = new UserLogHome();
            Map<String, Integer> mappa = ulh.glossarySource(this.startdate, this.enddate);
            String graph02data = " [ ";
            String graph02label = " [ ";
            graph02color = " [ ";
            for(String k : sourcelist)
            {
                if(mappa.keySet().contains(k))
                {
                    graph02data += " " + mappa.get(k) + ", ";
                    graph02label += " \"" + k + "\", ";
                    graph02color += " \"" + graph02map.get(k) + "\", ";
                }
            }
            graph02data += " 		]";
            graph02label += " 		]";
            graph02color += " 		]";
            graph02.setData(graph02data);
            graph02.setLabel(graph02label);

        }
        return graph02;
    }

    public String getGraph02color()
    {
        return graph02color;
    }
    */
    public Fields getGraph01()
    {
        if(graph01 == null)
        {
            graph01 = new Fields();
            UserLogHome ulh = new UserLogHome();
            Map<String, Integer> mappa = ulh.actionPerDay(this.startdate, this.enddate);
            String graph01label = " [ ";
            ArrayList<String> miedate = new ArrayList<String>();
            try
            {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                Date startDate = formatter.parse(this.startdate);
                Date endDate = formatter.parse(this.enddate);
                Calendar start = Calendar.getInstance();
                start.setTime(startDate);
                Calendar end = Calendar.getInstance();
                end.setTime(endDate);
                for(Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime())
                {
                    graph01label += " \"" + formatter.format(date) + "\", ";
                    miedate.add("" + formatter.format(date));
                }
            }
            catch(ParseException e)
            {
                log.fatal("fatal", e);
            }
            graph01label += " 		]";

            String graph01data = " [ ";
            for(String miadata : miedate)
            {
                if(mappa.keySet().contains(miadata))
                    graph01data += " " + mappa.get(miadata) + ", ";
                else
                    graph01data += " 0, ";
            }
            graph01data += " 		]";
            graph01.setData(graph01data);
            graph01.setLabel(graph01label);
        }
        return graph01;
    }

    public boolean isViewpanel1()
    {
        return viewpanel1;
    }

    public String getTopbar01data()
    {
        if(topbar01data.compareTo("") == 0)
        {
            int search = 0;
            int glossary = 0;
            int resource = 0;
            int system = 0;
            UserLogHome ulh = new UserLogHome();
            Map<String, Integer> mappa = ulh.actionCount(this.startdate, this.enddate);

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
            topbar01data = "var mydata = [ " + glossary + "," + search + "," + system + "," + resource + " ];";
        }
        return topbar01data;
    }

    public void updateGloss()
    {
        viewpanel1 = true;
        UserLogHome ulh = new UserLogHome();
        totalconcepts = ulh.getTotalConcepts(this.startdate, this.enddate);
        totalterms = ulh.getTotalTerms(this.startdate, this.enddate);
    }

    public ArrayList<TotalData> getSummary()
    {
        // System.out.println("CHAIMATO SUMMARY");
        if(summarylist == null)
        {
            summarylist = new ArrayList<TotalData>();
            // System.out.println("DENTRO SUMMARY");
            String[] userid = new String[] { "10413", "10430", "10429", "10443", "10411", "10152", "10111", "10117", "10428", "10113" };
            UserLogHome ulh = new UserLogHome();
            ;

            Map<String, Integer[]> summap = ulh.getSummary2(startdate, enddate);

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
    /*
    
    
    public Collection<HashMap<String, Object>> getTrackerStatistics()
    {
        if(trackerStatistic == null)
        {
            try
            {
                trackerStatistic = new ArrayList<>(ulh.getTrackerStatisticsPerUser(selectedCourse, startdate, enddate));
            }
            catch(Exception e)
            {
                log.error("Could not get statistic for course " + selectedCourse, e);
            }
        }
        return trackerStatistic;
    }
    */

    public void setSelectedCourse(Course selectedCourse)
    {
        this.selectedCourse = selectedCourse;
    }

    public Course getSelectedCourse()
    {
        return selectedCourse;
    }
}
