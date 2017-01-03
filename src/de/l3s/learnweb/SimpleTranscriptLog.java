package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;

public class SimpleTranscriptLog implements Serializable
{
    private static final long serialVersionUID = -5957511920632610709L;

    private int userId;
    private int resourceId;
    private int selectionCount;
    private int deselectionCount;
    private int userAnnotationCount;

    // cached values
    private transient User user;
    private transient Resource resource;

    public SimpleTranscriptLog()
    {
    }

    public SimpleTranscriptLog(int userId, int resourceId, int selectionCount, int deselectionCount, int userAnnotationCount)
    {
        this.userId = userId;
        this.resourceId = resourceId;
        this.selectionCount = selectionCount;
        this.deselectionCount = deselectionCount;
        this.userAnnotationCount = userAnnotationCount;
    }

    public User getUser() throws SQLException
    {
        if(null == user)
            user = Learnweb.getInstance().getUserManager().getUser(getUserId());
        return user;
    }

    public Resource getResource() throws SQLException
    {
        if(null == resource)
            resource = Learnweb.getInstance().getResourceManager().getResource(resourceId);
        return resource;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public int getSelectionCount()
    {
        return selectionCount;
    }

    public int getDeselectionCount()
    {
        return deselectionCount;
    }

    public int getUserAnnotationCount()
    {
        return userAnnotationCount;
    }

}
