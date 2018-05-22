package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.model.UploadedFile;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

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
    public String description; // TODO why is this public?
    private UploadedFile multimediaFile;
    private int resourceId;
    private int userId;
    int glossaryId = 0;

    // cached values
    private transient User user;

    public int getGlossaryId()
    {
        return glossaryId;
    }

    public void setGlossaryId(int glossaryId)
    {
        this.glossaryId = glossaryId;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = StringUtils.defaultString(description);
    }

    public List<LanguageItem> getSecondLanguageItems()
    {
        return secondLanguageItems;
    }

    public List<LanguageItem> getFirstLanguageItems()
    {
        return firstLanguageItems;
    }

    public void setSecondLanguageItems(List<LanguageItem> secondLanguageItems)
    {
        this.secondLanguageItems = secondLanguageItems;
    }

    public void setFirstLanguageItems(List<LanguageItem> firstLanguageItems)
    {
        this.firstLanguageItems = firstLanguageItems;
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
        this.userId = user.getId();
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
        this.user = null;
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
