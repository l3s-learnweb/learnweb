package de.l3s.learnweb.resource.speechRepository;

public class SpeechRepositoryEntity
{
    private int id;
    private String title;
    private String url;
    private String rights;
    private String date;
    private String description;
    private String notes;
    private String imageLink;
    private String videoLink;
    private Integer duration;
    private String language;
    private String level;
    private String use;
    private String type;
    private String domains;
    private String terminology;
    private int learnwebResourceId;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setId(String id)
    {
        this.id = Integer.parseInt(id);
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getRights()
    {
        return rights;
    }

    public void setRights(String rights)
    {
        this.rights = rights;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    public String getImageLink()
    {
        return imageLink;
    }

    public void setImageLink(String imageLink)
    {
        this.imageLink = imageLink;
    }

    public String getVideoLink()
    {
        return videoLink;
    }

    public void setVideoLink(String videoLink)
    {
        this.videoLink = videoLink;
    }

    public Integer getDuration()
    {
        return duration;
    }

    public void setDuration(Integer duration)
    {
        this.duration = duration;
    }

    public void setDuration(String durationStr)
    {
        String[] tokens = durationStr.split(":");
        int duration = 0, multiply = 0;
        for(int i = tokens.length - 1; i >= 0; --i)
        {
            duration += Integer.parseInt(tokens[i]) * Math.pow(60, multiply++);
        }

        this.duration = duration;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public String getUse()
    {
        return use;
    }

    public void setUse(String use)
    {
        this.use = use;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getDomains()
    {
        return domains;
    }

    public void setDomains(String domains)
    {
        this.domains = domains;
    }

    public String getTerminology()
    {
        return terminology;
    }

    public void setTerminology(String terminology)
    {
        this.terminology = terminology;
    }

    public int getLearnwebResourceId()
    {
        return learnwebResourceId;
    }

    public void setLearnwebResourceId(int learnwebResourceId)
    {
        this.learnwebResourceId = learnwebResourceId;
    }
}
