package de.l3s.learnweb.resource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.util.HasId;
import de.l3s.util.StringHelper;

public class File implements Serializable, HasId {
    private static final long serialVersionUID = 6573841175365679674L;

    private static final Pattern NAME_FORBIDDEN_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|]");

    public enum TYPE {
        SYSTEM_FILE, // for example course header images
        ORGANISATION_BANNER,
        PROFILE_PICTURE,
        THUMBNAIL_SMALL, // thumbnail0
        THUMBNAIL_MEDIUM, // thumbnail2
        THUMBNAIL_LARGE, // thumbnail4
        MAIN, // the file that can be downloaded/viewed
        ORIGINAL, // if the file was converted the original file should be moved to this location
        DOC_HISTORY, // previous version of an office resource
        DOC_CHANGES, // zip file with changes for office resource
        OBSOLETE; // something that probably will not be used anymore, like thumbnail1 and thumbnail3

        public boolean in(TYPE... types) {
            return Arrays.stream(types).anyMatch(type -> type == this);
        }
    }

    private int id;
    private boolean deleted;
    private int resourceId;
    private String name;
    private String mimeType;
    private TYPE type;
    private String url;
    private LocalDateTime lastModified;

    private java.io.File actualFile;
    private boolean actualFileExists = true; // when the actual file doesn't exist it is replaced by an error image

    public File() {
    }

    public File(final TYPE type, final String name, final String mimeType) {
        this.type = type;
        this.name = name;
        this.mimeType = mimeType;
    }

    public File(final File file) {
        this.id = 0;
        this.deleted = file.isDeleted();
        this.resourceId = file.getResourceId();
        this.type = file.getType();
        this.name = file.getName();
        this.mimeType = file.getMimeType();
        this.lastModified = file.getLastModified();
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

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getUrl() {
        if (url == null) {
            url = "../download/" + id + "/" + StringHelper.urlEncode(name);
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAbsoluteUrl() {
        return Learnweb.config().getServerUrl() + getUrl().substring(2);
    }

    /**
     * @return The actual file in the file system
     */
    public java.io.File getActualFile() {
        if (null == actualFile && id != 0) {
            actualFile = new java.io.File(Learnweb.config().getFileManagerFolder(), id + ".dat");
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
        return "File [fileId=" + id + ", name=" + name + ", mimeType=" + mimeType + ", resourceId=" + resourceId +
            ", type=" + type + ", url=" + url + ", lastModified=" + lastModified + "]";
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }
}
