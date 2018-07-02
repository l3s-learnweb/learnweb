package de.l3s.learnweb.resource.glossaryNew;

import java.io.Serializable;

import javax.validation.constraints.Size;

public class GlossaryEntry implements Serializable
{
    private static final long serialVersionUID = 1251808024273639912L;

    private int id;
    private String description;
    @Size(max = 100)
    private String topicOne;
    private String topicTwo;
    private String topicThree;
    private boolean descriptionPasted = false;

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

    public boolean isDescriptionPasted()
    {
        return descriptionPasted;
    }

    public void setDescriptionPasted(boolean onPasteDescription)
    {
        this.descriptionPasted = onPasteDescription;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

}
