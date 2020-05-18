package de.l3s.learnweb.resource.office.history.model;

/**
 * Object used to send data to `docEditor.setHistoryData` method.
 */
public class HistoryData {
    private String key;
    private String url;
    private String changesUrl;
    private HistoryData previous;
    private int version;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getChangesUrl() {
        return changesUrl;
    }

    public void setChangesUrl(final String changesUrl) {
        this.changesUrl = changesUrl;
    }

    public HistoryData getPrevious() {
        return previous;
    }

    public void setPrevious(final HistoryData previous) {
        this.previous = previous;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }
}
