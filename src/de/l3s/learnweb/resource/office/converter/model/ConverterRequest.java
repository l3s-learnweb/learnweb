package de.l3s.learnweb.resource.office.converter.model;

public class ConverterRequest {
    private final String fileType;
    private final String outputType;
    private final String title;
    private final String url;
    private final String key;

    private OfficeThumbnailParams thumbnail;

    public ConverterRequest(String fileType, String outputType, String title, String url, String key) {
        this.fileType = fileType.replace(".", "");
        this.outputType = outputType.replace(".", "");
        this.title = title;
        this.url = url;
        this.key = key;

    }

    public ConverterRequest(String fileType, String outputType, String title, String url, String key, OfficeThumbnailParams thumbnailParams) {
        this.fileType = fileType.replace(".", "");
        this.outputType = outputType.replace(".", "");
        this.title = title;
        this.url = url;
        this.thumbnail = thumbnailParams;
        this.key = key;
    }

    public String getFileType() {
        return fileType;
    }

    public String getOutputType() {
        return outputType;
    }

    public OfficeThumbnailParams getThumbnail() {
        return thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "ConverterRequest [fileType=" + fileType + ", outputType=" + outputType + ", title=" + title + ", url=" + url + ", key=" + key + ", thumbnail=" + thumbnail + "]";
    }
}
