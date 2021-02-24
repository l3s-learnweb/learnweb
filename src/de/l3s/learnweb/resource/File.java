package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.util.HasId;
import de.l3s.util.StringHelper;

public class File implements Serializable, HasId, Cloneable {
    private static final long serialVersionUID = 6573841175365679674L;

    private static final Pattern NAME_FORBIDDEN_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|]");

    public enum TYPE {
        UNKNOWN, // 0
        THUMBNAIL_SQUARED, // 1
        THUMBNAIL_SMALL, // 2
        THUMBNAIL_MEDIUM, // 3
        FILE_MAIN, // 4 the file that can be downloaded/viewed
        THUMBNAIL_LARGE, // 5
        THUMBNAIL_VERY_SMALL, // 6
        FILE_ORIGINAL, // 7 if the file was converted the original file should be moved to this location
        PROFILE_PICTURE, // 8
        SYSTEM_FILE, // 9 for example course header images
        HISTORY_FILE, // 10 previous version of an office file
        CHANGES, // 11 zip file with changes for office resources
    }

    private int fileId = -1;
    private String name;
    private String mimeType;
    private Integer resourceId;
    private TYPE type;
    private String url;
    private boolean downloadLogActivated = false;
    private LocalDateTime lastModified;

    private java.io.File actualFile;
    private boolean actualFileExists = true; // when the actual file doesn't exist it is replaced by an error image. For this reason we have to store if the file exists

    public File() {
    }

    public File(final TYPE type, final String name, final String mimeType) {
        this.type = type;
        this.name = name;
        this.mimeType = mimeType;
    }

    public File(final File file) {
        this.type = file.getType();
        this.name = file.getName();
        this.mimeType = file.getMimeType();
    }

    @Override
    public int getId() {
        return fileId;
    }

    public void setId(int fileId) {
        this.fileId = fileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = NAME_FORBIDDEN_CHARACTERS.matcher(StringUtils.defaultString(name)).replaceAll("_"); // replace invalid characters
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    public String getUrl() {
        if (url == null) {
            url = "../download/" + fileId + "/" + StringHelper.urlEncode(name);
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isDownloadLogActivated() {
        return downloadLogActivated;
    }

    public void setDownloadLogActivated(boolean downloadLogActivated) {
        this.downloadLogActivated = downloadLogActivated;
    }

    /**
     * @return The actual file in the file system
     */
    public java.io.File getActualFile() {
        if (null == actualFile && fileId > 1) {
            actualFile = new java.io.File(Learnweb.config().getFileManagerFolder(), fileId + ".dat");
        } else if (null == actualFile) {
            throw new IllegalStateException("Either the file has not been saved yet, use FileManager.save() first. Or the file isn't present on this machine.");
        }
        return actualFile;
    }

    protected void setActualFile(java.io.File actualFile) {
        this.actualFile = actualFile;
    }

    public boolean exists() {
        return actualFileExists;
    }

    public void setExists(boolean actualFileExists) {
        this.actualFileExists = actualFileExists;
    }

    public OutputStream getOutputStream() throws FileNotFoundException {
        // if the actual file doesn't exist it is replaced with an error image. Make sure that this image isn't changed
        if (!exists()) {
            throw new FileNotFoundException();
        }
        return new FileOutputStream(getActualFile());
    }

    public InputStream getInputStream() {
        try {
            return new FileInputStream(getActualFile());
        } catch (FileNotFoundException e) { // the FileManager has to take care that this exception never occurs
            throw new IllegalStateException(e);
        }
    }

    public long getLength() {
        return getActualFile().length();
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "File [fileId=" + fileId + ", name=" + name + ", mimeType=" + mimeType + ", resourceId=" + resourceId + ", type=" + type + ", url=" + url + ", lastModified=" + lastModified + "]";
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public File clone() {
        File destination = new File();
        if (name != null) {
            destination.setName(name);
        }
        if (url != null) {
            destination.setUrl(url);
        }
        if (type != null) {
            destination.setType(type);
        }
        if (lastModified != null) {
            destination.setLastModified(lastModified);
        }
        if (mimeType != null) {
            destination.setMimeType(mimeType);
        }
        destination.setResourceId(resourceId);
        return destination;
    }
}
