package de.l3s.learnweb;

import java.io.Serializable;

public class Thumbnail implements Comparable<Thumbnail>, Serializable
{
    private static final long serialVersionUID = -792701713759619246L;

    private int width;
    private int height;
    private final String url;
    private final int fileId;

    public Thumbnail(String url, int width, int height, int fileId)
    {
	this.url = url;
	this.width = width;
	this.height = height;
	this.fileId = fileId;
    }

    public Thumbnail(String url, int width, int height)
    {
	this.url = url;
	this.width = width;
	this.height = height;
	this.fileId = 0;
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
    protected int getFileId()
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

    public String toHTML()
    {
	return "<img src=\"" + url + "\" width=\"" + width + "\" height=\"" + height + "\" />";
    }
}
