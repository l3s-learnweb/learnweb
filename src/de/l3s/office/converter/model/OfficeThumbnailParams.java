package de.l3s.office.converter.model;

public class OfficeThumbnailParams
{
    private boolean first = true;

    private int aspect;

    private int height = 1024; //the thumbnail height in pixels

    private int width = 1280; //the thumbnail width in pixels 

    public boolean isFirst()
    {
        return first;
    }

    public void setFirst(boolean first)
    {
        this.first = first;
    }

    public int getAspect()
    {
        return aspect;
    }

    public void setAspect(int aspect)
    {
        this.aspect = aspect;
    }

    public int getHeight()
    {
        return height;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

}
