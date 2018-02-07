package jcdashboard.bean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import jcdashboard.model.Description;
import jcdashboard.model.Fields;
import jcdashboard.model.UsesTable;
import jcdashboard.model.dao.UserLogHome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ManagedBean(name = "studentview")
@ViewScoped
public class StudentviewBean
{

    private Fields graph01 = null;

    private Fields graph02 = null;
    private String graph02color = "";

    private String topbar01data = "";
    private boolean viewpanel1 = true;
    private Integer totalconcepts = 0;
    private Integer totalterms = 0;

    private Integer sid = 10410;

    private List<Description> dlist = null;
    private Fields fields;
    private Fields proxylog;

    private String startdate = "2017-03-02";
    private String enddate = "2017-04-04"; // new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    Map<String, String> graph02map = new HashMap<String, String>();

    public String getGraph02color()
    {
        return graph02color;
    }

    public StudentviewBean()
    {
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

    public Integer getSid()
    {
        return sid;
    }

    public List<Entry<String, Integer>> getProxyloglist()
    {
        UserLogHome ulh = new UserLogHome();
        return entriesSortedByValues(ulh.proxySources(sid, this.startdate, this.enddate));
    }

    public void updateCharts()
    {
        graph01 = null;
        graph02 = null;
        proxylog = null;
        dlist = null;
        fields = null;
        topbar01data = "";
        totalconcepts = 0;
        totalterms = 0;
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

    private int util1(JSONObject root, String[] valori, int level)
    {
        int indice = -1;
        try
        {
            if(root.has("children"))
            {
                JSONArray arr = root.getJSONArray("children");
                for(int j = 0; j < arr.length(); j++)
                {
                    if(arr.getJSONObject(j).getString("name").compareTo(valori[level]) == 0)
                    {
                        indice = j;
                        break;
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
        return indice;
    }

    public Fields getProxylog()
    {
        if(proxylog == null)
        {
            proxylog = new Fields();
            UserLogHome ulh = new UserLogHome();
            // Map<String,Integer> mappa=ulh.proxySources(sid);
            List<Entry<String, Integer>> mappa = entriesSortedByValues(ulh.proxySources(sid, this.startdate, this.enddate));

            List<String> sb = new ArrayList<String>();
            try
            {
                InputStream in = this.getClass().getResourceAsStream("/jcdashboard/Dashboard_website_types.csv");
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                String line = br.readLine();

                while(line != null)
                {
                    sb.add(line);
                    // sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                br.close();
            }
            catch(IOException e1)
            {
                e1.printStackTrace();
            }

            try
            {
                JSONObject root = new JSONObject();

                root.put("name", "root");

                for(Entry<String, Integer> e : mappa)
                {
                    int idx = -1;

                    for(String v : sb)
                        if(v.startsWith(e.getKey()))
                        {
                            idx = sb.indexOf(v);
                        }

                    String[] valori; // =new String[5];
                    if(idx >= 0)
                    {
                        valori = sb.get(idx).split(";");
                        if(valori.length == 5)
                        {
                            int indice = util1(root, valori, 4);
                            if(indice == -1)
                            {
                                JSONObject obj = new JSONObject();
                                obj.put("name", "" + valori[4]);
                                root.append("children", obj);
                            }
                            else
                            {
                                //System.out.println(""+indice);
                                //System.out.println(""+root.getJSONArray("children").get(indice));
                                JSONObject l1 = root.getJSONArray("children").getJSONObject(indice);
                                int indice1 = util1(l1, valori, 3);
                                if(indice1 == -1)
                                {
                                    JSONObject obj = new JSONObject();
                                    obj.put("name", "" + valori[3]);
                                    l1.append("children", obj);
                                } // else{
                                indice1 = util1(l1, valori, 3);
                                JSONObject l2 = l1.getJSONArray("children").getJSONObject(indice1);
                                int indice2 = util1(l2, valori, 2);
                                if(indice2 == -1)
                                {
                                    JSONObject obj = new JSONObject();
                                    obj.put("name", "" + valori[2]);
                                    JSONObject objsite = new JSONObject();
                                    objsite.put("name", "" + valori[0]);
                                    objsite.put("size", "" + e.getValue());
                                    obj.append("children", objsite);
                                    l2.append("children", obj);
                                }
                                else
                                {
                                    JSONObject l3 = l2.getJSONArray("children").getJSONObject(indice2);
                                    JSONObject objsite = new JSONObject();
                                    objsite.put("name", "" + valori[0]);
                                    objsite.put("size", "" + e.getValue());
                                    l3.append("children", objsite);
                                }
                            }
                        }

                    }
                    else
                    {

                    }
                }

                System.out.println(root);
                System.out.println("------------------");
            }
            catch(JSONException e1)
            {
                e1.printStackTrace();
            }

            String plabel = " [ ";
            String pdata = " [ ";
            for(Entry<String, Integer> e : mappa)
            {
                pdata += " " + e.getValue() + ", ";
                plabel += " \"" + e.getKey() + "\", ";
            }
            plabel += " 		]";
            pdata += " 		]";
            proxylog.setLabel(plabel);
            proxylog.setData(pdata);
        }
        return proxylog;
    }

    static <K, V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K, V> map)
    {

        List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(map.entrySet());

        Collections.sort(sortedEntries, new Comparator<Entry<K, V>>()
        {
            @Override
            public int compare(Entry<K, V> e1, Entry<K, V> e2)
            {
                return e2.getValue().compareTo(e1.getValue());
            }
        });

        return sortedEntries;
    }

    public Fields getFields()
    {
        if(fields == null)
        {
            fields = new Fields();
            UserLogHome ulh = new UserLogHome();
            UsesTable ut = ulh.fields(sid, this.startdate, this.enddate);
            fields.setLabel("[ 'pronounciation \uf137', 'acronym', 'phraseology', 'uses', 'source' ]");
            fields.setData(" [ " + ut.getPronounciation() + "," + ut.getAcronym() + "," + ut.getPhraseology() + "," + ut.getUses() + "," + ut.getSource() + " ] ");
        }
        return fields;
    }

    public void setSid(Integer sid)
    {
        this.sid = sid;
    }

    public String getDescriptionsavg()
    {
        UserLogHome ulh = new UserLogHome();
        List<String> descriptions = ulh.descritpions(this.sid, this.startdate, this.enddate);
        float wordCount = 0;
        for(String description : descriptions)
        {
            String[] wordArray = description.trim().split("\\s+");
            wordCount += wordArray.length;
        }
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return twoDForm.format(wordCount / descriptions.size());
    }

    public List<Description> getDescriptions()
    {
        if(dlist == null)
        {
            UserLogHome ulh = new UserLogHome();
            dlist = new ArrayList<Description>();
            List<String> descriptions = ulh.descritpions(this.sid, this.startdate, this.enddate);
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

    /* public String getGraph03label() {
    	UserLogHome ulh=new UserLogHome();
    	Map<String, Integer> mappa = ulh.userGlossary();
    	graph03label= " [ ";
    	for (String k : mappa.keySet()){
    		graph03label+=" \""+k+"\", ";
    	}
    	graph03label+=" 		]";
    	
    	return graph03label;
    }
    
    public String getGraph03terms() {
    	UserLogHome ulh=new UserLogHome();
    	Map<String, Integer> mappa = ulh.userGlossaryTerm();
    	graph03terms= " [ ";
    	for (String k : mappa.keySet()){
    		graph03terms+=" "+mappa.get(k)+", ";
    	}
    	graph03terms+=" 		]";
    	
    	return graph03terms;
    }
    
    public String getGraph03concepts() {
    	UserLogHome ulh=new UserLogHome();
    	Map<String, Integer> mappa = ulh.userGlossary();
    	graph03concepts= " [ ";
    	for (String k : mappa.keySet()){
    		graph03concepts+=" "+mappa.get(k)+", ";
    	}
    	graph03concepts+=" 		]";
    	
    	return graph03concepts;
    } */

    public Integer getTotalconcepts()
    {
        UserLogHome ulh = new UserLogHome();
        totalconcepts = ulh.getTotalConcepts(this.sid, this.startdate, this.enddate);
        return totalconcepts;
    }

    public Integer getTotalterms()
    {
        UserLogHome ulh = new UserLogHome();
        totalterms = ulh.getTotalTerms(this.sid, this.startdate, this.enddate);
        return totalterms;
    }

    public Integer getTotalsource()
    {
        UserLogHome ulh = new UserLogHome();
        return ulh.getTotalSource(this.sid, this.startdate, this.enddate);
    }

    public String getRatiotc()
    {
        float res = (float) totalterms / totalconcepts;
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return twoDForm.format(res);
    }

    public Fields getGraph02()
    {
        if(graph02 == null)
        {
            graph02 = new Fields();
            String[] sourcelist = new String[] { "EMPTY", "Wikipedia", "encyclopaedia", "monolingual dictionary", "bilingual dictionary", "Linguee or Reverso", "institutional website", "patients' websites and blogs", "scientific/academic publication", "glossary", "other" };
            UserLogHome ulh = new UserLogHome();
            Map<String, Integer> mappa = ulh.glossarySource(this.sid, this.startdate, this.enddate);
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

    public Fields getGraph02new()
    {
        if(graph02 == null)
        {
            ArrayList<String> graph02data = new ArrayList<String>();
            graph02 = new Fields();
            String[] sourcelist = new String[] { "EMPTY", "Wikipedia", "encyclopaedia", "monolingual dictionary", "bilingual dictionary", "Linguee or Reverso", "institutional website", "patients' websites and blogs", "scientific/academic publication", "glossary", "other" };
            UserLogHome ulh = new UserLogHome();
            Map<String, Integer> mappa = ulh.glossarySource(this.sid, this.startdate, this.enddate);
            double graph02color = 0.1;
            for(String k : sourcelist)
            {
                if(mappa.keySet().contains(k))
                {
                    graph02data.add("{\"label\": \"" + k + "\",\"value\": " + mappa.get(k) + ",\"color\": \"rgba(38, 185, 154, " + graph02color + ")\"}");
                    graph02color += 0.1;
                }
            }
            graph02.setData(String.join(" , ", graph02data));
        }
        return graph02;
    }

    public Fields getGraph01()
    {
        if(graph01 == null)
        {
            graph01 = new Fields();
            UserLogHome ulh = new UserLogHome();
            Map<String, Integer> mappa = ulh.actionPerDay(sid, this.startdate, this.enddate);
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
                e.printStackTrace();
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
            Integer search = 0;
            Integer glossary = 0;
            Integer resource = 0;
            Integer system = 0;
            UserLogHome ulh = new UserLogHome();
            Map<String, Integer> mappa = ulh.actionCount(this.sid, this.startdate, this.enddate);

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
        // System.out.println(topbar01data);
        return topbar01data;
    }

    public void updateGloss()
    {
        viewpanel1 = true;
        UserLogHome ulh = new UserLogHome();
        totalconcepts = ulh.getTotalConcepts(this.startdate, this.enddate);
        totalterms = ulh.getTotalTerms(this.startdate, this.enddate);

    }

    public void setTopbar01data(String topbar01data)
    {
        this.topbar01data = topbar01data;
    }

}
