package de.l3s.learnweb.resource.glossary;

public class GlossaryEntry_NEW
{
    private int entryId;
    private String description;
    private String topicOne;
    private String topicTwo;
    private String topicThree;
    private boolean onPasteDescription = false;

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getTopicOne()
    {
        return topicOne;
    }

    public void setTopicOne(String topicOne)
    {
        this.topicOne = topicOne;
    }

    public String getTopicTwo()
    {
        return topicTwo;
    }

    public void setTopicTwo(String topicTwo)
    {
        this.topicTwo = topicTwo;
    }

    public String getTopicThree()
    {
        return topicThree;
    }

    public void setTopicThree(String topicThree)
    {
        this.topicThree = topicThree;
    }

    public int getEntryId()
    {
        return entryId;
    }

    public void setEntryId(int entryId)
    {
        this.entryId = entryId;
    }

    public boolean isOnPasteDescription()
    {
        return onPasteDescription;
    }

    public void setOnPasteDescription(boolean onPasteDescription)
    {
        this.onPasteDescription = onPasteDescription;
    }

}
