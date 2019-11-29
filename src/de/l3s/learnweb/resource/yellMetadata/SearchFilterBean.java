package de.l3s.learnweb.resource.yellMetadata;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class SearchFilterBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 987526892886356642L;
    private static final Logger log = Logger.getLogger(SearchFilterBean.class);

    private String[] selectedAuthors;
    private String[] selectedTargets;
    private String[] selectedPurposes;
    private String[] selectedLanguages;
    private String[] selectedLevels;

    private String selectedLang = "";

    private List<String> authors;
    private List<String> targets;
    private List<String> purposes;
    private List<String> langs;
    private List<String> levels;

    public SearchFilterBean()
    {
        // do nothing constructor
    }

    @PostConstruct
    public void init()
    {
        //media sources and authors from database

        //author filter values (must get it from db)
        authors = new ArrayList<>();
        authors.add("Shakespear");
        authors.add("Brian C.");
        authors.add("Chloe H.");
        authors.add("Thomas Hardy");

        //target learner values (fixed values)
        targets = new ArrayList<>();
        targets.add("Teachers");
        targets.add("Adult learners");
        targets.add("Teens");
        targets.add("Young learners");
        targets.add("Pre-school");

        //purposes values
        try
        {
            // convert purpose list to string list of purpose names
            Stream<Purpose> purposesStream = getLearnweb().getPurposeManager().getPurposes(getUser()).stream();
            purposes = purposesStream.map(Purpose::getName).collect(Collectors.toList());
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }

        /*

        purposes.add("Listening");
        purposes.add("Reading");
        purposes.add("Writing");
        purposes.add("Speaking");
        purposes.add("Class Activities");
        purposes.add("Lesson Plans");
        purposes.add("Cross-curricular resources / CLIL");
        purposes.add("Story-telling");
        purposes.add("Plurallingualism");
        purposes.add("Special Learning Needs");
        purposes.add("Assessment");
        purposes.add("Teacher Education Resources");
        purposes.add("Other");*/

        //languages values (fixed values)
        langs = new ArrayList<>();
        langs.add("English");
        langs.add("Italian");
        langs.add("German");

        //language level values (fixed values)
        levels = new ArrayList<>();
        levels.add("C2");
        levels.add("C1");
        levels.add("B2");
        levels.add("B1");
        levels.add("A2");
        levels.add("A1");
    }

    public List<String> completePurpose(String query)
    {
        if(StringUtils.isEmpty(query))
            return purposes;

        List<String> results = new ArrayList<>();
        for(String purpose : purposes)
        {
            if(StringUtils.containsIgnoreCase(purpose, query))
                results.add(purpose);
        }

        return results;
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
}
