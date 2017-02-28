package de.l3s.learnweb;

import java.sql.SQLException;
import java.util.List;

import org.primefaces.model.UploadedFile;

import de.l3s.glossary.LanguageItems;

public class GlossaryEntry
{
    private List<LanguageItems> ItalianItems;
    private List<LanguageItems> UkItems;
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

    public List<LanguageItems> getItalianItems()
    {
        return ItalianItems;

    }

    public void setItalianItems(List<LanguageItems> itItems)
    {
        this.ItalianItems = itItems;

    }

    public List<LanguageItems> getUkItems()
    {

        return UkItems;

    }

    public void setUkItems(List<LanguageItems> ukItems)
    {

        this.UkItems = ukItems;

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
                // TODO Auto-generated catch block
                e.printStackTrace();
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
