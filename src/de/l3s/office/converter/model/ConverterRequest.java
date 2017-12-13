package de.l3s.office.converter.model;

public class ConverterRequest
{
    private String filetype;
    private String key;
    private String outputtype;
    private OfficeThumbnailParams thumbnail;
    private String title;
    private String url;

    public ConverterRequest(String filetype, String outputtype, String title, String url, String key)
    {
        this.filetype = filetype.replace(".", "");
        this.outputtype = outputtype.replace(".", "");
        this.title = title;
        this.url = url;
        this.key = key;

    }

    public ConverterRequest(String filetype, String outputtype, String title, String url, String key, OfficeThumbnailParams thumbnailParams)
    {
        this.filetype = filetype.replace(".", "");
        this.outputtype = outputtype.replace(".", "");
        this.title = title;
        this.url = url;
        this.thumbnail = thumbnailParams;
        this.key = key;
    }

    public String getFiletype()
    {
        return filetype;
    }

    public String getOutputtype()
    {
        return outputtype;
    }

    public OfficeThumbnailParams getThumbnail()
    {
        return thumbnail;
    }

    public String getTitle()
    {
        return title;
    }

    public String getUrl()
    {
        return url;
    }

    public String getKey()
    {
        return key;
    }

}
