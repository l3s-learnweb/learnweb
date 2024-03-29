package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.util.HasId;

public class File implements Serializable, HasId {
    @Serial
    private static final long serialVersionUID = 6573841175365679674L;

    public enum FileType {
        // general files
        SYSTEM_FILE, // for example course header images
        ORGANISATION_BANNER,
        COURSE_PICTURE,
        PROFILE_PICTURE,
        GROUP_PICTURE,
        // resource files
        THUMBNAIL_SMALL, // cropped to 160 x 120 px - the smallest thumbnail used on website
        THUMBNAIL_MEDIUM, // resized <= 280 x 210 px - resource preview image size
        THUMBNAIL_LARGE, // resized <= 2048 x 1536 px - FHD image size, used on resource page if other media type is not available
        MAIN, // the file that can be downloaded/viewed
        ORIGINAL, // if the file was converted the original file should be moved to this location
        // special resource files
        GLOSSARY, // glossary entry pictures
        DOC_HISTORY, // previous version of an office resource
        DOC_CHANGES; // zip file with changes for office resource

        public boolean isResourceFile() {
            return switch (this) {
                // case THUMBNAIL_SMALL: // Small thumbnail files which are shown during resource upload and thus are not connected to a resource yet.
                case THUMBNAIL_MEDIUM, THUMBNAIL_LARGE, MAIN, ORIGINAL -> true;
                default -> false;
            };
        }
    }

    // entity fields
    private int id;
    private FileType type;
    private String name;
    private String mimeType;
    private LocalDateTime createdAt;

    // runtime fields
    private String urlSuffix;
    private String absoluteUrl;
    private java.io.File actualFile;
    private Boolean exists; // `true` if the actual file exist on this machine

    public File(final FileType type, final String name, final String mimeType) {
        this.type = type;
        setName(name);
        this.mimeType = mimeType;
    }

    public File(final File file) {
        this.type = file.getType();
        this.name = file.getName();
        this.mimeType = file.getMimeType();
        this.createdAt = file.getCreatedAt();
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int fileId) {
        this.id = fileId;
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

    public final void setName(String name) {
        this.name = StringUtils.replaceChars(name, "\\/:*?\"<>|%", "_");
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSimpleUrl() {
        if (getType().isResourceFile()) {
            throw new BadRequestHttpException("Unable to verify resource access permission for the given file, invalid URL");
        }
        return "../download/" + getUrlSuffix();
    }

    public String getAbsoluteUrl() {
        if (absoluteUrl == null) {
            absoluteUrl = Learnweb.config().getServerUrl() + "/download/" + getUrlSuffix();
        }
        return absoluteUrl;
    }

    public String getResourceUrl(int resourceId) {
        if (resourceId == 0) {
            throw new BadRequestHttpException("Redundant resource used for the given file, invalid URL");
        }
        return "../download/" + resourceId + "/" + getUrlSuffix();
    }

    private String getUrlSuffix() {
        if (urlSuffix == null) {
            urlSuffix = id + "/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        }
        return urlSuffix;
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

    public FileInputStream getInputStream() {
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
        return id == file.id && type == file.type && Objects.equals(name, file.name) && Objects.equals(mimeType, file.mimeType)
            && Objects.equals(createdAt, file.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, name, mimeType, createdAt);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("type", type)
            .append("name", name)
            .append("mimeType", mimeType)
            .append("createdAt", createdAt)
            .toString();
    }
}
