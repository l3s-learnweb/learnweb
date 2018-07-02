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
    private String topicOne;
    private String topicTwo;
    private String topicThree;
    private String description;
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

    public String getTopicTwo()
    {
        return topicTwo;
    }

    public void setTopicTwo(String selectedTopicTwo)
    {
        if(selectedTopicTwo != null)
            this.topicTwo = selectedTopicTwo;
        else
            this.topicTwo = "";
    }

    public String getTopicOne()
    {
        return topicOne;
    }

    public void setTopicOne(String selectedTopicOne)
    {
        if(selectedTopicOne != null)
            this.topicOne = selectedTopicOne;
        else
            this.topicOne = "";
    }

    public String getTopicThree()
    {
        return topicThree;
    }

    public void setTopicThree(String selectedTopicThree)
    {
        if(selectedTopicThree != null)
            this.topicThree = selectedTopicThree;
        else
            this.topicThree = "";
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
