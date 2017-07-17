package de.l3s.learnweb.rm.beans;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;

import de.l3s.learnweb.beans.ApplicationBean;

@ManagedBean
//@RequestScoped

public class SearchFilterBean extends ApplicationBean
{
    private String[] selectedAuthors;
    private String[] selectedMtypes;
    private String[] selectedSources;
    private String[] selectedTargets;
    private String[] selectedPurposes;
    private String[] selectedLanguages;
    private String[] selectedLevels;

    private List<String> authors;
    private List<String> mtypes;
    private List<String> sources;
    private List<String> targets;
    private List<String> purposes;
    private List<String> langs;
    private List<String> levels;

    @PostConstruct

    public void init()
    {

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
        targets.add("Adults");
        targets.add("Teenagers");
        targets.add("Young learners (Elementary)");
        targets.add("Very young learners (Pre-school");

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

    //languages getter and setter
    public String[] getSelectedLanguages()
    {
        return selectedLanguages;
    }

    public void setSelectedLanguages(String[] selectedLanguages)
    {
        this.selectedLanguages = selectedLanguages;
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

}
