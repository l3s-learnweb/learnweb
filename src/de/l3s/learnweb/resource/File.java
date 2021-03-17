package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.util.HasId;

public class File extends Thumbnail implements HasId {
    private static final long serialVersionUID = 6573841175365679674L;

    public enum FileType {
        SYSTEM_FILE, // for example course header images
        ORGANISATION_BANNER,
        PROFILE_PICTURE,
        THUMBNAIL_SMALL, // cropped to 160 x 120 px - smallest thumbnail used on website
        THUMBNAIL_MEDIUM, // resized <= 280 x 210 px - resource preview image size
        THUMBNAIL_LARGE, // resized <= 2048 x 1536 px - FHD image size, used on resource page if other media type is not available
        MAIN, // the file that can be downloaded/viewed
        ORIGINAL, // if the file was converted the original file should be moved to this location
        DOC_HISTORY, // previous version of an office resource
        DOC_CHANGES; // zip file with changes for office resource

        public boolean in(FileType... values) {
            return Arrays.stream(values).anyMatch(value -> value == this);
        }
    }

    // entity fields
    private int id;
    private boolean deleted;
    private int resourceId;
    private FileType type;
    private String name;
    private String mimeType;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    // runtime fields
    private String absoluteUrl;
    private java.io.File actualFile;
    private Boolean exists; // `true` if the actual file exist on this machine

    public File() {
    }

    public File(final FileType type, final String name, final String mimeType) {
        this.type = type;
        setName(name);
        this.mimeType = mimeType;
    }

    public File(final FileType type, int resourceId, final String name, final String mimeType) {
        this.type = type;
        this.resourceId = resourceId;
        setName(name);
        this.mimeType = mimeType;
    }

    public File(final File file) {
        this.deleted = file.isDeleted();
        this.resourceId = file.getResourceId();
        this.type = file.getType();
        this.name = file.getName();
        this.mimeType = file.getMimeType();
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int fileId) {
        this.id = fileId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = StringUtils.replaceChars(name, "\\/:*?\"<>|", "_");
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String getUrl() {
        if (url == null) {
            url = "../download/" + id + "/" + (name != null ? URLEncoder.encode(name, StandardCharsets.UTF_8) : "unknown");
        }
        return url;
    }

    public String getAbsoluteUrl() {
        if (absoluteUrl == null) {
            absoluteUrl = Learnweb.config().getServerUrl() + getUrl().substring(2);
        }
        return absoluteUrl;
    }

    /**
     * @return The actual file in the file system
     */
    public java.io.File getActualFile() {
        if (null == actualFile) {
            if (id == 0) {
                throw new IllegalStateException("The file has not been saved yet, use FileDao.save() first");
            }

            actualFile = new java.io.File(Learnweb.config().getFileManagerFolder(), id + ".dat");
        }
        return actualFile;
    }

    public boolean isExists() {
        if (exists == null) {
            exists = getActualFile().exists();
        }
        return exists;
    }

    public InputStream getInputStream() {
        try {
            return new FileInputStream(getActualFile());
        } catch (FileNotFoundException e) { // the FileManager has to take care that this exception never occurs
            exists = false;
            throw new IllegalStateException("The file isn't present on this machine", e);
        }
    }

    public long getLength() {
        return getActualFile().length();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final File file = (File) o;
        return id == file.id && deleted == file.deleted && resourceId == file.resourceId && type == file.type && Objects.equals(name, file.name)
            && Objects.equals(mimeType, file.mimeType) && Objects.equals(updatedAt, file.updatedAt) && Objects.equals(createdAt, file.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, deleted, resourceId, type, name, mimeType, updatedAt, createdAt);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("deleted", deleted)
            .append("resourceId", resourceId)
            .append("name", name)
            .append("mimeType", mimeType)
            .append("type", type)
            .append("url", url)
            .append("updatedAt", updatedAt)
            .append("createdAt", createdAt)
            .toString();
    }
}
