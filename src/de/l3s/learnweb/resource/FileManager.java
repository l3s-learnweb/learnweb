package de.l3s.learnweb.resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.Sql;
import de.l3s.util.StringHelper;

/**
 * DAO for the File class.
 *
 * @author Philipp
 */
public class FileManager {
    private static final Logger log = LogManager.getLogger(FileManager.class);

    // if you change this, remember to change createFile()
    private static final String COLUMNS = "file_id, resource_id, resource_file_number, name, mime_type, log_actived, timestamp";

    private final Learnweb learnweb;
    private final ICache<File> cache;
    private final java.io.File folder;
    private final String urlPattern;
    private String basePath;
    private java.io.File fileNotFoundErrorImage;

    public FileManager(Learnweb learnweb) throws SQLException {
        Properties properties = learnweb.getProperties();
        int cacheSize = Integer.parseInt(properties.getProperty("FILE_MANAGER_CACHE_SIZE"));

        this.learnweb = learnweb;
        this.urlPattern = properties.getProperty("FILE_MANAGER_URL_PATTERN");
        this.cache = cacheSize == 0 ? new DummyCache<>() : new Cache<>(cacheSize);
        this.folder = new java.io.File(properties.getProperty("FILE_MANAGER_FOLDER").trim());
        setServerUrl(learnweb.getServerUrl());

        if (!folder.exists()) {
            throw new RuntimeException("Folder '" + properties.getProperty("FILE_MANAGER_FOLDER") + "' does not exist.");
        } else if (!folder.canRead()) {
            throw new RuntimeException("Can't read from folder '" + properties.getProperty("FILE_MANAGER_FOLDER") + "'");
        } else if (!folder.canWrite()) {
            throw new RuntimeException("Can't write into folder '" + properties.getProperty("FILE_MANAGER_FOLDER") + "'");
        }

    }

    @Deprecated
    public void setFileNotFoundErrorImage(java.io.File fileNotFoundErrorImage) {
        this.fileNotFoundErrorImage = fileNotFoundErrorImage;
    }

    public void setServerUrl(String serverUrl) {

        this.basePath = serverUrl + urlPattern;
    }

    /**
     * Get a File by its id.
     *
     * @return null if not found
     */
    public File getFileById(int id) throws SQLException {
        File file = cache.get(id);

        if (null != file) {
            return file;
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_file WHERE file_id = ?");
        select.setInt(1, id);
        ResultSet rs = select.executeQuery();
        if (rs.next()) {
            file = createFile(rs);
        }
        select.close();

        return file;
    }

    public List<File> getFilesByResource(int resourceId) throws SQLException {
        List<File> files = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_file WHERE resource_id = ? AND deleted = 0 ORDER by resource_file_number, timestamp");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            files.add(createFile(rs));
        }
        select.close();

        return files;
    }

    public List<File> getAllFiles() throws SQLException {
        List<File> files = new LinkedList<>();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_file WHERE deleted = 0 order by resource_id desc");

        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            files.add(createFile(rs));
        }
        select.close();

        return files;
    }

    /*
    public static void main(String[] args) throws SQLException
    {
        // delete files which are not stored on the server

        FileManager fm = Learnweb.getInstance().getFileManager();
        List<File> files = fm.getAllFiles();

        HashSet<Integer> set = new HashSet<Integer>();

        for(File file : files) {
            if(file.actualFile == null) {
                set.add(file.getResourceId());
                // fm.delete(file);
            }
        }

        Iterator<Integer> iter = set.iterator();

        // delete the resources
        ResourceManager rm = Learnweb.getInstance().getResourceManager();

        while(iter.hasNext()) {
            Integer id = iter.next();

            if(id == 0)
            continue;

            rm.deleteResourcePermanent(id);
        }
    }
    */

    public File save(File file) throws SQLException {
        if (file.getId() < 0) {
            throw new IllegalArgumentException("This method can only update existing files");
        }

        try {

            return save(file, null);
        } catch (IOException e) {
            log.error("this should never happen", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves the file to the database.
     * If the file is not yet stored at the database, a new record will be created and the returned file contains the new id.
     */
    public File save(File file, InputStream inputStream) throws SQLException, IOException {
        if (file.getLastModified() == null) {
            file.setLastModified(new Date());
        }

        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_file` (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

        if (file.getId() < 0) { // the file is not yet stored at the database
            replace.setNull(1, java.sql.Types.INTEGER);
        } else {
            replace.setInt(1, file.getId());
        }
        replace.setInt(2, file.getResourceId());
        replace.setInt(3, file.getType().ordinal());
        replace.setString(4, file.getName());
        replace.setString(5, file.getMimeType());
        replace.setInt(6, file.isDownloadLogActivated() ? 1 : 0);
        replace.setTimestamp(7, Sql.convertDateTime(file.getLastModified()));
        replace.executeUpdate();

        if (file.getId() < 0) { // it's a new file -> get the assigned id
            ResultSet rs = replace.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("database error: no id generated");
            }
            file.setId(rs.getInt(1));
            file.setActualFile(createActualFile(file));
            file.setUrl(createUrl(file));
            file = cache.put(file); // add the new file to the cache
        }
        replace.close();

        if (inputStream != null) {
            // copy the data into the file
            OutputStream outputStream = new FileOutputStream(file.getActualFile());
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
        }

        return file;
    }

    public void addFileToResource(File file, Resource resource) throws SQLException {
        PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_file` SET `resource_id` = ? WHERE file_id = ?");
        update.setInt(1, resource.getId());
        update.setInt(2, file.getId());
        update.executeUpdate();
        update.close();
    }

    public void addFilesToResource(Collection<File> files, Resource resource) throws SQLException {
        if (files.isEmpty()) {
            return;
        }

        List<Integer> fileIds = new ArrayList<>(files.size());

        for (File file : files) {
            file.setResourceId(resource.getId());
            fileIds.add(file.getId());
        }

        Statement stmt = learnweb.getConnection().createStatement();
        stmt.executeUpdate("UPDATE lw_file SET resource_id = " + resource.getId() + " WHERE file_id IN(" + StringHelper.implodeInt(fileIds, ",") + ")");
        stmt.close();
    }

    public void delete(int fileId) throws SQLException {
        delete(getFileById(fileId));
    }

    public void delete(File file) throws SQLException {
        PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_file` SET deleted = 1 WHERE file_id = ?");
        update.setInt(1, file.getId());
        update.executeUpdate();
        update.close();

        cache.remove(file.getId());

        if (file.exists()) {
            try {
                file.getActualFile().delete();
            } catch (Throwable e) {
                log.error("Could not delete file: " + file.getId(), e);
            }
        }
    }

    private File createFile(ResultSet rs) throws SQLException {
        int fileId = rs.getInt("file_id");
        File file = cache.get(fileId);

        if (null != file) {
            return file;
        }

        file = new File();
        file.setId(fileId);
        file.setResourceId(rs.getInt("resource_id"));
        file.setType(TYPE.values()[rs.getInt("resource_file_number")]);
        file.setName(rs.getString("name"));
        file.setMimeType(rs.getString("mime_type"));
        file.setDownloadLogActivated(rs.getBoolean("log_actived"));
        file.setActualFile(createActualFile(file));
        file.setUrl(createUrl(file));
        file.setLastModified(new Date(rs.getTimestamp("timestamp").getTime()));

        if (!file.getActualFile().exists()) {
            log.warn("Can't find file '{}' for resource {}", file.getActualFile().getAbsolutePath(), file.getResourceId());

            file.setExists(false);

            // if(file.getMimeType().startsWith("image/")) {
            file.setActualFile(fileNotFoundErrorImage); // new java.io.File(folder, "404-no-file.png"));
            file.setMimeType("image/png");
            //}  else file.setActualFile(null);
        }

        file = cache.put(file);
        return file;
    }

    private String createUrl(File file) {
        return createUrl(file.getId(), file.getName());
    }

    /**
     * This method should have package scope. Retrieve a full File object and use file.getUrl()
     */
    @Deprecated
    public String createUrl(int fileId, String fileName) {
        return basePath + fileId + "/" + StringHelper.urlEncode(fileName);
    }

    /**
     * Returns the download url for a specific file and appends "thumbnail.png".
     * this method doesn't check if the file exists or the mime type is correct
     */
    public String getThumbnailUrl(int fileId, int size) {
        return basePath + fileId + "/thumbnail" + size + ".png";
    }

    private java.io.File createActualFile(File file) {
        return new java.io.File(folder, file.getId() + ".dat");
    }

    /**
     * @return number of cached objects
     */
    public int getCacheSize() {
        return cache.size();
    }

    public void resetCache() {
        cache.clear();
    }

    public File copy(File source) {
        File destination = new File();
        if (source.getName() != null) {
            destination.setName(source.getName());
        }
        if (source.getUrl() != null) {
            destination.setUrl(source.getUrl());
        }
        if (source.getType() != null) {
            destination.setType(source.getType());
        }
        if (source.getLastModified() != null) {
            destination.setLastModified(source.getLastModified());
        }
        if (source.getMimeType() != null) {
            destination.setMimeType(source.getMimeType());
        }
        destination.setResourceId(source.getResourceId());
        return destination;
    }

    public static void main(String[] args) throws SQLException {
        URL fileNotFoundResource = FileManager.class.getResource("/resources/images/file-not-found.png");

        if (null == fileNotFoundResource) {
            throw new RuntimeException("Can't find file-not-found.png");
        }

        /*
        Learnweb learnweb = Learnweb.getInstance();

        findAbandonedFiles();

        learnweb.onDestroy();
        */
    }

    protected static void findAbandonedFiles() throws SQLException {
        FileManager fm = Learnweb.getInstance().getFileManager();

        PreparedStatement update = Learnweb.getInstance().getConnection().prepareStatement("update lw_file set missing = 1 where file_id = ?");

        for (File file : fm.getAllFiles()) {
            if (!file.exists()) {

                update.setInt(1, file.getId());
                update.executeUpdate();
            }

        }

        update.close();

    }

}
