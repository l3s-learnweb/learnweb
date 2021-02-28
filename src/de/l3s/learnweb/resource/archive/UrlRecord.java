package de.l3s.learnweb.resource.archive;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import de.l3s.util.URL;

/**
 * @author Philipp Kemkes
 */
public class UrlRecord {
    private long id = -1L;
    private URL url;
    private LocalDateTime firstCapture;
    private LocalDateTime lastCapture;
    private Instant crawlDate = Instant.EPOCH;
    private boolean allCapturesFetched = false;

    private short statusCode = -3;
    private Instant statusCodeDate = Instant.EPOCH;
    private String content;

    public UrlRecord(URL asciiUrl) {
        this.url = asciiUrl;
    }

    public boolean isArchived() {
        return firstCapture != null && lastCapture != null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public LocalDateTime getFirstCapture() {
        return firstCapture;
    }

    public void setFirstCapture(LocalDateTime firstCapture) {
        this.firstCapture = firstCapture;
    }

    public LocalDateTime getLastCapture() {
        return lastCapture;
    }

    public void setLastCapture(LocalDateTime lastCapture) {
        this.lastCapture = lastCapture;
    }

    public Instant getCrawlDate() {
        return crawlDate;
    }

    public void setCrawlDate(Instant lastUpdate) {
        this.crawlDate = lastUpdate;
    }

    public boolean isAllCapturesFetched() {
        return allCapturesFetched;
    }

    public void setAllCapturesFetched(boolean allCapturesFetched) {
        this.allCapturesFetched = allCapturesFetched;
    }

    public short getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(short statusCode) {
        this.statusCode = statusCode;
    }

    public Instant getStatusCodeDate() {
        return statusCodeDate;
    }

    public void setStatusCodeDate(Instant statusCodeDate) {
        this.statusCodeDate = statusCodeDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UrlRecord urlRecord = (UrlRecord) o;
        return allCapturesFetched == urlRecord.allCapturesFetched && statusCode == urlRecord.statusCode
            && Objects.equals(url, urlRecord.url) && Objects.equals(firstCapture, urlRecord.firstCapture)
            && Objects.equals(lastCapture, urlRecord.lastCapture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, firstCapture, lastCapture, allCapturesFetched, statusCode);
    }

    public boolean isOffline() {
        return (statusCode < 200 || statusCode >= 400)
            && statusCode != 403 && statusCode != 650 && statusCode != 999 && statusCode != 606 && statusCode != 603 && statusCode != 429 && statusCode != -1;
    }

    @Override
    public String toString() {
        return "UrlRecord [id=" + id + ", url=" + url + ", firstCapture=" + firstCapture + ", lastCapture=" + lastCapture + ", crawlDate=" + crawlDate +
            ", allCapturesFetched=" + allCapturesFetched + ", statusCode=" + statusCode + ", statusCodeDate=" + statusCodeDate + "]";
    }
}
