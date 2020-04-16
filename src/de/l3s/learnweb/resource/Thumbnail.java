package de.l3s.learnweb.resource;

import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Thumbnail implements Comparable<Thumbnail>, Serializable
{
    private static final long serialVersionUID = -792701713759619246L;
    private static final Logger log = LogManager.getLogger(Thumbnail.class);

    private int width = 0;
    private int height = 0;
    private final String url;
    private final int fileId;
    // TODO Oleh: add flag which indicates when the Thumbnail is a placeholder

    public Thumbnail(String url, int width, int height, int fileId)
    {
        this.url = url;
        this.fileId = fileId;

        if(width < 0)
            log.warn("Ignore negative width: " + width);
        else
            this.width = width;

        if(height < 0)
            log.warn("Ignore negative height: " + height);
        else
            this.height = height;
    }

    public Thumbnail(String url, int width, int height)
    {
        this(url, width, height, 0);
    }

    /**
     * Returns a resized version f the thumbnail
     *
     * @param maxWidth
     * @param maxHeight
     */
    public Thumbnail resize(int maxWidth, int maxHeight)
    {
        Thumbnail tn = this.clone();

        if(maxWidth < 2 || maxHeight < 2)
            throw new IllegalArgumentException();

        if(tn.width > maxWidth)
        {
            double ratio = (double) maxWidth / (double) tn.width;
            tn.height = (int) Math.ceil(tn.height * ratio);
            tn.width = maxWidth;
        }

        if(tn.height > maxHeight)
        {
            double ratio = (double) maxHeight / (double) tn.height;
            tn.width = (int) Math.ceil(tn.width * ratio);
            tn.height = maxHeight;
        }

        return tn;
    }

    @Override
    public int compareTo(Thumbnail t)
    {
        if(width < t.width)
        {
            return -1;
        }
        if(width > t.width)
        {
            return 1;
        }
        if(height < t.height)
        {
            return -1;
        }
        if(height > t.height)
        {
            return 1;
        }
        return url.compareTo(t.url);
    }

    public int getHeight()
    {
        return height;
    }

    public String getUrl()
    {
        return url;
    }

    public int getWidth()
    {
        return width;
    }

    /**
     * The file id is 0 if the thumbnail is not stored in learnweb
     *
     * @return
     */
    public int getFileId()
    {
        return fileId;
    }

    @Override
    public String toString()
    {
        return "Thumbnail [width=" + width + ", height=" + height + ", url=" + url + ", fileId=" + fileId + "]";
    }

    @Override
    public Thumbnail clone()
    {
        return new Thumbnail(url, width, height, fileId);
    }

    /**
     *
     * @return HTML img tag for this thumbnail
     */
    public String getHtml()
    {
        return "<img src=\"" + url + "\" width=\"" + width + "\" height=\"" + height + "\" />";
    }
}
