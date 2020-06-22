package de.l3s.learnweb.resource.archive;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class ArchiveUrl implements Serializable {
    private static final long serialVersionUID = 63994605834754451L;

    private final SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

    private String archiveUrl;
    private String fileUrl;
    private Date timestamp;
    private long simhash;
    private int shingleId;

    public ArchiveUrl(String archiveUrl, Date timestamp) {
        this.archiveUrl = archiveUrl;
        this.timestamp = timestamp;
    }

    public ArchiveUrl(String archiveUrl, Date timestamp, long simhash) {
        this.archiveUrl = archiveUrl;
        this.timestamp = timestamp;
        this.simhash = simhash;
    }

    public ArchiveUrl(String archiveUrl, String fileUrl, Date timestamp) {
        this.archiveUrl = archiveUrl;
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
    }

    public ArchiveUrl(String archiveUrl, Date timestamp, long simhash, int shingleId) {
        this.archiveUrl = archiveUrl;
        this.timestamp = timestamp;
        this.simhash = simhash;
        this.shingleId = shingleId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getShingleId() {
        return shingleId;
    }

    public String getArchiveUrl() {
        return archiveUrl;
    }

    public void setArchiveUrl(String archiveUrl) {
        this.archiveUrl = archiveUrl;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestampInArchiveFormat() {
        return df.format(timestamp);
    }

    public long getSimhash() {
        return simhash;
    }

    public void setSimhash(long simhash) {
        this.simhash = simhash;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ArchiveUrl that = (ArchiveUrl) o;
        return StringUtils.equalsIgnoreCase(archiveUrl, that.archiveUrl)
            && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(archiveUrl, timestamp);
    }

    @Override
    public String toString() {
        return "[" + this.archiveUrl + ", " + this.timestamp + "]";
    }

}
