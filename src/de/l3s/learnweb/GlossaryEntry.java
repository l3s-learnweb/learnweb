package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.primefaces.model.UploadedFile;

import de.l3s.glossary.LanguageItem;

public class GlossaryEntry implements Serializable
{
    private static final long serialVersionUID = -6600840950470704444L;
    private static final Logger log = Logger.getLogger(GlossaryEntry.class);

    private List<LanguageItem> secondLanguageItems;
    private List<LanguageItem> firstLanguageItems;
    private String fileName;
    private String selectedTopicOne;
    private String selectedTopicTwo;
    private String selectedTopicThree;
    public String description;
    private UploadedFile multimediaFile;
    private int resourceId;
    private int userId;
    int glossaryId = 0;

    public int getGlossaryId()
    {
        return glossaryId;
    }

    public void setGlossaryId(int glossaryId)
    {
        this.glossaryId = glossaryId;
    }

    // cached values
    private transient User user;

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        if(description != null)
            this.description = description;
        else
            this.description = "";

    }

    public List<LanguageItem> getSecondLanguageItems()
    {
        return secondLanguageItems;

    }

    public void setSecondLanguageItems(List<LanguageItem> itItems)
    {
        this.secondLanguageItems = itItems;

    }

    public List<LanguageItem> getFirstLanguageItems()
    {

        return firstLanguageItems;

    }

    public void setFirstLanguageItems(List<LanguageItem> ukItems)
    {

        this.firstLanguageItems = ukItems;

    }

    public String getSelectedTopicTwo()
    {
        return selectedTopicTwo;
    }

    public void setSelectedTopicTwo(String selectedTopicTwo)
    {
        if(selectedTopicTwo != null)
            this.selectedTopicTwo = selectedTopicTwo;
        else
            this.selectedTopicTwo = "";
    }

    public String getSelectedTopicOne()
    {
        return selectedTopicOne;
    }

    public void setSelectedTopicOne(String selectedTopicOne)
    {
        if(selectedTopicOne != null)
            this.selectedTopicOne = selectedTopicOne;
        else
            this.selectedTopicOne = "";
    }

    public String getSelectedTopicThree()
    {
        return selectedTopicThree;
    }

    public void setSelectedTopicThree(String selectedTopicThree)
    {
        if(selectedTopicThree != null)
            this.selectedTopicThree = selectedTopicThree;
        else
            this.selectedTopicThree = "";
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public UploadedFile getMultimediaFile()
    {

        return multimediaFile;

    }

    public void setMultimediaFile(UploadedFile multimediaFile)
    {
        this.multimediaFile = multimediaFile;
    }

    public User getUser()
    {
        if(user == null)
        {
            try
            {
                user = Learnweb.getInstance().getUserManager().getUser(userId);
            }
            catch(SQLException e)
            {
                log.error("unhandled error", e);
            }
        }
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;

    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;

    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

}
