package de.l3s.learnweb.resource.office.converter.model;

public class ConverterResponse {
    private String fileUrl;

    private int percent;

    private Boolean endConvert;

    private Integer error;

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public Boolean isEndConvert() {
        return endConvert;
    }

    public void setEndConvert(Boolean endConvert) {
        this.endConvert = endConvert;
    }

    public Integer getError() {
        return error;
    }

    public void setError(Integer error) {
        this.error = error;
    }

}
