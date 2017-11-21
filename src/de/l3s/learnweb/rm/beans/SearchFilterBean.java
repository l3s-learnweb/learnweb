package de.l3s.learnweb.rm.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.rm.CategoryBottom;
import de.l3s.learnweb.rm.CategoryMiddle;
import de.l3s.learnweb.rm.CategoryTop;

@ManagedBean
@ViewScoped
public class SearchFilterBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 987526892886356642L;
    private static final Logger log = Logger.getLogger(SearchFilterBean.class);

    private String[] selectedAuthors;
    private String[] selectedMtypes;
    private String[] selectedSources;
    private String[] selectedTargets;
    private String[] selectedPurposes;
    private String[] selectedLanguages;
    private String[] selectedLevels;

    private String selectedLang = "";
    private String selectedCattop = "";
    private String selectedCatmid = "";
    private String selectedCatbot = "";

    private List<String> authors;
    private List<String> mtypes;
    private List<String> sources;
    private List<String> targets;
    private List<String> purposes;
    private List<String> langs;
    private List<String> levels;
    private List<CategoryTop> catTops;
    private List<CategoryMiddle> catMids;
    private List<CategoryBottom> catBots;
    private List<String> cattops;
    private List<String> catmids;
    private List<String> catbots;
    private int topDefaultId = 1;

    public SearchFilterBean()
    {
        // do nothing constructor
    }

    @PostConstruct
    public void init()
    {
        //reset variable lists
        targets = null;
        levels = null;
        purposes = null;
        mtypes = null;
        langs = null;

        //media sources and authors from database 

        //get all top categories
        try
        {
            catTops = getLearnweb().getCategoryManager().getAllTopCategories();
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }

        cattops = new ArrayList<String>();
        for(int i = 0; i < catTops.size(); i++)
        {
            cattops.add(catTops.get(i).getCattop_name());
        }

        //middle categories
        try
        {
            catMids = getLearnweb().getCategoryManager().getAllMiddleCategoriesByCattopID(topDefaultId);
        }
        catch(SQLException e1)
        {
            addFatalMessage(e1);
        }

        catmids = new ArrayList<String>();
        for(int i = 0; i < catMids.size(); i++)
        {
            if(!(catMids.get(i).getCatmid_name().equalsIgnoreCase("x")))
            {
                catmids.add(catMids.get(i).getCatmid_name());
            }
        }

        //author filter values (must get it from db) 
        authors = new ArrayList<String>();
        authors.add("Shakespear");
        authors.add("Brian C.");
        authors.add("Chloe H.");
        authors.add("Thomas Hardy");

        //mtype filter values (fixed values) 
        mtypes = new ArrayList<String>();
        mtypes.add("Text");
        mtypes.add("Video");
        mtypes.add("Image");
        mtypes.add("Game");
        mtypes.add("App");

        //source filter values (must get it from db) 
        sources = new ArrayList<String>();
        sources.add("BBC");
        sources.add("Cambridge");
        sources.add("TED");
        sources.add("Guardian");

        //target learner values (fixed values) 
        targets = new ArrayList<String>();
        targets.add("Teachers");
        targets.add("Adult learners");
        targets.add("Teenage learners");
        targets.add("Young learners (Elementary)");
        targets.add("Very young learners (Pre-school)");

        //purposes values (fixed values) 
        purposes = new ArrayList<String>();
        purposes.add("Listening Skills");
        purposes.add("Reading Skills");
        purposes.add("Writing Skills");
        purposes.add("Speaking Skills");
        purposes.add("Class Activities");
        purposes.add("Lesson Plans");
        purposes.add("Cross-curricular resources / CLIL");
        purposes.add("Story-telling");
        purposes.add("Plurallingualism");
        purposes.add("Special Learning Needs");
        purposes.add("Assessment");
        purposes.add("Teacher Education Resources");
        purposes.add("Other");

        //languages values (fixed values) 
        langs = new ArrayList<String>();
        langs.add("English");
        langs.add("Italian");
        langs.add("German");

        //language level values (fixed values) 
        levels = new ArrayList<String>();
        levels.add("C2");
        levels.add("C1");
        levels.add("B2");
        levels.add("B1");
        levels.add("A2");
        levels.add("A1");

        log.debug("init: " + levels);
    }

    //mtypes getter and setter
    public List<String> getMtypes()
    {
        return mtypes;
    }

    public void setSelectedMtypes(String[] selectedMtypes)
    {
        this.selectedMtypes = selectedMtypes;
    }

    public String[] getSelectedMtypes()
    {
        return selectedMtypes;
    }

    //authors getter and setter
    public String[] getSelectedAuthors()
    {
        return selectedAuthors;
    }

    public void setSelectedAuthors(String[] selectedAuthors)
    {
        this.selectedAuthors = selectedAuthors;
    }

    public List<String> getAuthors()
    {
        return authors;
    }

    //sources getter and setter
    public String[] getSelectedSources()
    {
        return selectedSources;
    }

    public void setSelectedSources(String[] selectedSources)
    {
        this.selectedSources = selectedSources;
    }

    public List<String> getSources()
    {
        return sources;
    }

    //targets getter and setter
    public String[] getSelectedTargets()
    {
        return selectedTargets;
    }

    public void setSelectedTargets(String[] selectedTargets)
    {
        this.selectedTargets = selectedTargets;
    }

    public List<String> getTargets()
    {
        return targets;
    }

    //purposes getter and setter
    public String[] getSelectedPurposes()
    {
        return selectedPurposes;
    }

    public void setSelectedPurposes(String[] selectedPurposes)
    {
        this.selectedPurposes = selectedPurposes;
    }

    public List<String> getPurposes()
    {
        return purposes;
    }

    public List<String> getLangs()
    {
        return langs;
    }

    //levels getter and setter
    public String[] getSelectedLevels()
    {
        return selectedLevels;
    }

    public void setSelectedLevels(String[] selectedLevels)
    {
        this.selectedLevels = selectedLevels;
    }

    public List<String> getLevels()
    {
        return levels;
    }

    //language getter and setter (when choosing a single language to save) 
    public String getSelectedLang()
    {
        return selectedLang;
    }

    public void setSelectedLang(String selectedLang)
    {
        this.selectedLang = selectedLang;
    }

    //langs getter and setter (lang option display purpose) 
    public String[] getSelectedLanguages()
    {
        return selectedLanguages;
    }

    public void setSelectedLanguages(String[] selectedLanguages)
    {
        this.selectedLanguages = selectedLanguages;
    }

    //categories setter and getter
    public String getSelectedCattop()
    {
        return selectedCattop;
    }

    public void setSelectedCattop(String selectedCattop)
    {
        this.selectedCattop = selectedCattop;
    }

    public String getSelectedCatmid()
    {
        return selectedCatmid;
    }

    public void setSelectedCatmid(String selectedCatmid)
    {
        this.selectedCatmid = selectedCatmid;
    }

    public String getSelectedCatbot()
    {
        return selectedCatbot;
    }

    public void setSelectedCatbot(String selectedCatbot)
    {
        this.selectedCatbot = selectedCatbot;
    }

    public List<String> getCattops()
    {
        return cattops;
    }

    public void setCattops(List<String> cattops)
    {
        this.cattops = cattops;
    }

    public List<String> getCatmids()
    {
        return catmids;
    }

    public void setCatmids(List<String> catmids)
    {
        this.catmids = catmids;
    }

    public List<String> getCatbots()
    {
        return catbots;
    }

    public List<String> showCatbots(String query)
    {
        return catbots;
    }

    public void setCatbots(List<String> catbots)
    {
        this.catbots = catbots;
    }

    //populate middle category list when top category is selected
    public void topCatChanged(ValueChangeEvent e)
    { //reset catMids and catmids
        catMids = null;
        catmids = null;

        String topcat = (String) e.getNewValue();
        int cattopId = 0;
        try
        {
            cattopId = getLearnweb().getCategoryManager().getCategoryTopByName(topcat);
        }
        catch(SQLException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if(cattopId > 0)
        {
            try
            {
                catMids = getLearnweb().getCategoryManager().getAllMiddleCategoriesByCattopID(cattopId);
            }
            catch(SQLException e1)
            {

                e1.printStackTrace();
            }

            catmids = new ArrayList<String>();
            for(int i = 0; i < catMids.size(); i++)
            {
                if(!(catMids.get(i).getCatmid_name().equalsIgnoreCase("x")))
                {
                    catmids.add(catMids.get(i).getCatmid_name());
                }
            }
        }
        else //if top category name returned with invalid id number < 1
        {
            new IllegalArgumentException("invalid top category Id was given: " + selectedCattop).printStackTrace();
        }

    }

    //populate bottom category when middle category is selected! attention: user need to be able to add a new bottom cat so adding "add a new category" in the string
    public void midCatChanged(ValueChangeEvent e)
    { //reset catBots and catbots
        catBots = null;
        catbots = null;

        String midcat = (String) e.getNewValue();
        log.info("midcat value is " + midcat);

        int catmidId = 0;
        try
        {
            catmidId = getLearnweb().getCategoryManager().getCategoryMiddleByName(midcat);
        }
        catch(SQLException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        log.info("catmidId is " + catmidId);

        if(catmidId > 0)
        {
            try
            {
                catBots = getLearnweb().getCategoryManager().getAllBottomCategoriesByCatmidID(catmidId);
            }
            catch(SQLException e1)
            {

                e1.printStackTrace();
            }

            catbots = new ArrayList<String>();
            for(int i = 0; i < catBots.size(); i++)
            {
                if(!(catBots.get(i).getCatbot_name().equalsIgnoreCase("x")))
                {
                    catbots.add(catBots.get(i).getCatbot_name());
                }
            }
        }
        else //if middle category name returned with invalid id number < 1
        {
            new IllegalArgumentException("invalid mid category Id was given: " + selectedCatmid).printStackTrace();
        }
        //log.info("catbots length is " + catbots.size());
    }

    public void botCatChanged(ValueChangeEvent e)
    {
        String botcat = (String) e.getNewValue();
        if(botcat.equalsIgnoreCase("add a new category"))
        {
            log.info("adding a new category has been chosen!");
        }
    }
}
