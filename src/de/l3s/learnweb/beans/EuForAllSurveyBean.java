package de.l3s.learnweb.beans;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;

@ViewScoped
@ManagedBean
public class EuForAllSurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -1811030091337893627L;
    private static final Logger log = Logger.getLogger(EuForAllSurveyBean.class);

    private int resourceId;
    private String name;
    private String surname;
    private String studentID;
    private String gender;
    private String qualification;
    private String studies;
    private String firstLang;
    private String multimodality;
    private int blogsProd;
    private int blogsUnder;
    private int videosProd;
    private int videosUnder;
    private int fanvidProd;
    private int fanvidUnder;
    private int interacProd;
    private int interacUnder;
    private int webProd;
    private int webUnder;
    private String blogCreate;
    private String vidComm;
    private String vidUpOnline;
    private String vidUpYes;
    private String fanvidUpOnline;
    private String fanvidUpYes;
    private int levelExpert = -1;
    private int usefulness = -1;
    private String[] activities;
    private String[] usefulAreas;
    private String assess;
    private String blogYes;
    private String multiYes;
    private String otherLang;
    private String level;
    private int groupId; // group id of the resource used only for the logger

    public String getBlogCreate()
    {
        return blogCreate;
    }

    public void setBlogCreate(String blogCreate)
    {
        this.blogCreate = blogCreate;
    }

    public String getBlogYes()
    {
        return blogYes;
    }

    public void setBlogYes(String blogYes)
    {
        this.blogYes = blogYes;
    }

    public int getVideosProd()
    {
        return videosProd;
    }

    public void setVideosProd(int videosProd)
    {
        this.videosProd = videosProd;
    }

    public int getVideosUnder()
    {
        return videosUnder;
    }

    public void setVideosUnder(int videosUnder)
    {
        this.videosUnder = videosUnder;
    }

    public int getFanvidProd()
    {
        return fanvidProd;
    }

    public void setFanvidProd(int fanvidProd)
    {
        this.fanvidProd = fanvidProd;
    }

    public int getFanvidUnder()
    {
        return fanvidUnder;
    }

    public void setFanvidUnder(int fanvidUnder)
    {
        this.fanvidUnder = fanvidUnder;
    }

    public int getInteracProd()
    {
        return interacProd;
    }

    public void setInteracProd(int interacProd)
    {
        this.interacProd = interacProd;
    }

    public int getInteracUnder()
    {
        return interacUnder;
    }

    public void setInteracUnder(int interacUnder)
    {
        this.interacUnder = interacUnder;
    }

    public int getWebProd()
    {
        return webProd;
    }

    public void setWebProd(int webProd)
    {
        this.webProd = webProd;
    }

    public int getWebUnder()
    {
        return webUnder;
    }

    public void setWebUnder(int webUnder)
    {
        this.webUnder = webUnder;
    }

    public int getBlogsProd()
    {
        return blogsProd;
    }

    public void setBlogsProd(int blogsProd)
    {
        this.blogsProd = blogsProd;
    }

    public int getBlogsUnder()
    {
        return blogsUnder;
    }

    public void setBlogsUnder(int blogsUnder)
    {
        this.blogsUnder = blogsUnder;
    }

    public String getMultimodality()
    {
        return multimodality;
    }

    public void setMultimodality(String multimodality)
    {
        this.multimodality = multimodality;
    }

    public String getMultiYes()
    {
        return multiYes;
    }

    public void setMultiYes(String multiYes)
    {
        this.multiYes = multiYes;
    }

    public String getFirstLang()
    {
        return firstLang;
    }

    public void setFirstLang(String firstLang)
    {
        this.firstLang = firstLang;
    }

    public String getOtherLang()
    {
        return otherLang;
    }

    public void setOtherLang(String otherLang)
    {
        this.otherLang = otherLang;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public String getStudies()
    {
        return studies;
    }

    public void setStudies(String studies)
    {
        this.studies = studies;
    }

    public String getQualification()
    {
        return qualification;
    }

    public void setQualification(String qualification)
    {
        this.qualification = qualification;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getYear()
    {
        return year;
    }

    public void setYear(String year)
    {
        this.year = year;
    }

    private String year;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSurname()
    {
        return surname;
    }

    public void setSurname(String surname)
    {
        this.surname = surname;
    }

    public String getStudentID()
    {
        return studentID;
    }

    public void setStudentID(String studentID)
    {
        this.studentID = studentID;
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0)
        {
            ;

            try
            {
                Resource resource = getLearnweb().getResourceManager().getResource(resourceId);
                groupId = resource.getGroupId();
                log(Action.glossary_open, groupId, resourceId);
            }
            catch(Exception e)
            {
                log.error("Couldn't log glossary action; resource: " + resourceId);
            }

        }
    }

    public String getVidComm()
    {
        return vidComm;
    }

    public void setVidComm(String vidComm)
    {
        this.vidComm = vidComm;
    }

    public String getVidUpOnline()
    {
        return vidUpOnline;
    }

    public void setVidUpOnline(String vidUpOnline)
    {
        this.vidUpOnline = vidUpOnline;
    }

    public String getVidUpYes()
    {
        return vidUpYes;
    }

    public void setVidUpYes(String vidUpYes)
    {
        this.vidUpYes = vidUpYes;
    }

    public String getFanvidUpOnline()
    {
        return fanvidUpOnline;
    }

    public void setFanvidUpOnline(String fanvidUpOnline)
    {
        this.fanvidUpOnline = fanvidUpOnline;
    }

    public String getFanvidUpYes()
    {
        return fanvidUpYes;
    }

    public void setFanvidUpYes(String fanvidUpYes)
    {
        this.fanvidUpYes = fanvidUpYes;
    }

    public int getLevelExpert()
    {
        return levelExpert;
    }

    public void setLevelExpert(int levelExpert)
    {
        this.levelExpert = levelExpert;
    }

    public int getUsefulness()
    {
        return usefulness;
    }

    public void setUsefulness(int usefulness)
    {
        this.usefulness = usefulness;
    }

    public String[] getActivities()
    {
        return activities;
    }

    public void setActivities(String[] activities)
    {
        this.activities = activities;
    }

    public String getAssess()
    {
        return assess;
    }

    public void setAssess(String assess)
    {
        this.assess = assess;
    }

    public String[] getUsefulAreas()
    {
        return usefulAreas;
    }

    public void setUsefulAreas(String[] usefulAreas)
    {
        this.usefulAreas = usefulAreas;
    }
}
