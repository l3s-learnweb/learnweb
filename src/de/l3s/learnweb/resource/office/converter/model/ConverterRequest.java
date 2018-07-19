package de.l3s.learnweb.resource.office.converter.model;

public class ConverterRequest
{
    private String fileType;
    private String key;
    private String outputType;
    private OfficeThumbnailParams thumbnail;
    private String title;
    private String url;

    public ConverterRequest(String fileType, String outputType, String title, String url, String key)
    {
        this.fileType = fileType.replace(".", "");
        this.outputType = outputType.replace(".", "");
        this.title = title;
        this.url = url;
        this.key = key;

    }

    public ConverterRequest(String fileType, String outputType, String title, String url, String key, OfficeThumbnailParams thumbnailParams)
    {
        this.fileType = fileType.replace(".", "");
        this.outputType = outputType.replace(".", "");
        this.title = title;
        this.url = url;
        this.thumbnail = thumbnailParams;
        this.key = key;
    }

    public String getFileType()
    {
        return fileType;
    }

    public String getOutputType()
    {
        return outputType;
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
