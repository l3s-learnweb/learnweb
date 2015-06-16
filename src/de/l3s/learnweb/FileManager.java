package de.l3s.learnweb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.apache.log4j.Logger;

import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.StringHelper;

/**
 * DAO for the File class.
 * 
 * @author Philipp
 * 
 */
public class FileManager
{
    private final static Logger log = Logger.getLogger(FileManager.class);

    // if you change this, remember to change createFile()
    private final static String COLUMNS = "file_id, resource_id, resource_file_number, name, mime_type, log_actived, timestamp";

    private Learnweb learnweb;
    private ICache<File> cache;
    private final java.io.File folder;
    private final String urlPattern;
    private String baseUrl;

    protected FileManager(Learnweb learnweb) throws SQLException
    {
	Properties properties = learnweb.getProperties();
	int cacheSize = Integer.parseInt(properties.getProperty("FILE_MANAGER_CACHE_SIZE"));

	this.learnweb = learnweb;
	this.urlPattern = properties.getProperty("FILE_MANAGER_URL_PATTERN");
	this.cache = cacheSize == 0 ? new DummyCache<File>() : new Cache<File>(cacheSize);
	this.folder = new java.io.File(properties.getProperty("FILE_MANAGER_FOLDER"));

	setContextUrl(learnweb.getContextUrl());

	if(!folder.exists())
	    throw new RuntimeException("Folder '" + properties.getProperty("FILE_MANAGER_FOLDER") + "' does not exist.");
	else if(!folder.canRead())
	    throw new RuntimeException("Can't read from folder '" + properties.getProperty("FILE_MANAGER_FOLDER") + "'");
	else if(!folder.canWrite())
	    throw new RuntimeException("Can't write into folder '" + properties.getProperty("FILE_MANAGER_FOLDER") + "'");
    }

    /**
     * Get an File by his id
     * 
     * @param id
     * @return null if not found
     * @throws SQLException
     */
    public File getFileById(int id) throws SQLException
    {
	File file = cache.get(id);

	if(null != file)
	    return file;

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_file WHERE file_id = ? AND deleted = 0");
	select.setInt(1, id);
	ResultSet rs = select.executeQuery();
	if(rs.next())
	    file = createFile(rs);
	select.close();

	return file;
    }

    public List<File> getFilesByResource(int resourceId) throws SQLException
    {
	List<File> files = new LinkedList<File>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_file WHERE resource_id = ? AND deleted = 0 ORDER by resource_file_number, timestamp");
	select.setInt(1, resourceId);
	ResultSet rs = select.executeQuery();
	while(rs.next())
	    files.add(createFile(rs));
	select.close();

	return files;
    }

    public List<File> getAllFiles() throws SQLException
    {
	List<File> files = new LinkedList<File>();

	PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_file WHERE deleted = 0 order by resource_id");

	ResultSet rs = select.executeQuery();
	while(rs.next())
	    files.add(createFile(rs));
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

    for(File file : files)
    {
        if(file.actualFile == null)
        {
    	System.out.println("file fehlt resource: " + file.getResourceId());

    	set.add(file.getResourceId());

    	//fm.delete(file);
        }
    }

    Iterator<Integer> iter = set.iterator();

    // delete the resources 
    ResourceManager rm = Learnweb.getInstance().getResourceManager();

    while(iter.hasNext())
    {
        Integer id = iter.next();

        if(id == 0)
    	continue;

        rm.deleteResourcePermanent(id);

    }

    System.out.println("deleted " + set.size());
    }
    */

    /**
     * Saves the file to the database.
     * If the file is not yet stored at the database, a new record will be created and the returned file contains the new id.
     * 
     * @param file
     * @param inputStream
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public File save(File file, InputStream inputStream) throws SQLException, IOException
    {
	if(file.getLastModified() == null)
	    file.setLastModified(new Date());

	PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_file` (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

	if(file.getId() < 0) // the file is not yet stored at the database 			
	    replace.setNull(1, java.sql.Types.INTEGER);
	else
	    replace.setInt(1, file.getId());
	replace.setInt(2, file.getResourceId());
	replace.setInt(3, file.getResourceFileNumber());
	replace.setString(4, file.getName());
	replace.setString(5, file.getMimeType());
	replace.setInt(6, file.isDownloadLogActivated() ? 1 : 0);
	replace.setDate(7, new java.sql.Date(file.getLastModified().getTime()));
	replace.executeUpdate();

	if(file.getId() < 0) // it's a new file -> get the assigned id
	{
	    ResultSet rs = replace.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    file.setId(rs.getInt(1));
	    file.setActualFile(createActualFile(file));
	    file.setUrl(createUrl(file));
	    file = cache.put(file); // add the new file to the cache
	}
	replace.close();

	// copy the data into the file
	OutputStream outputStream = new FileOutputStream(file.getActualFile());
	IOUtils.copy(inputStream, outputStream);
	outputStream.close();

	return file;
    }

    public void addFileToResource(File file, Resource resource) throws SQLException
    {
	PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_file` SET `resource_id` = ? WHERE file_id = ?");
	update.setInt(1, resource.getId());
	update.setInt(2, file.getId());
	update.executeUpdate();
	update.close();
    }

    public void addFilesToResource(Collection<File> files, Resource resource) throws SQLException
    {
	if(files.size() == 0)
	    return;

	ArrayList<Integer> fileIds = new ArrayList<Integer>(files.size());

	for(File file : files)
	{
	    file.setResourceId(resource.getId());
	    fileIds.add(file.getId());
	}

	Statement stmt = learnweb.getConnection().createStatement();
	stmt.executeUpdate("UPDATE lw_file SET resource_id = " + resource.getId() + " WHERE file_id IN(" + StringHelper.implodeInt(fileIds, ",") + ")");
	stmt.close();
    }

    public void delete(int fileId) throws SQLException
    {
	delete(getFileById(fileId));
    }

    public void delete(File file) throws SQLException
    {
	PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_file` SET deleted = 1 WHERE file_id = ?");
	update.setInt(1, file.getId());
	update.executeUpdate();
	update.close();

	cache.remove(file.getId());

	//file.getActualFile().delete();
    }

    private File createFile(ResultSet rs) throws SQLException
    {
	int fileId = rs.getInt("file_id");
	File file = cache.get(fileId);

	if(null != file)
	    return file;

	file = new File();
	file.setId(fileId);
	file.setResourceId(rs.getInt("resource_id"));
	file.setResourceFileNumber(rs.getInt("resource_file_number"));
	file.setName(rs.getString("name"));
	file.setMimeType(rs.getString("mime_type"));
	file.setDownloadLogActivated(rs.getInt("log_actived") == 1);
	file.setActualFile(createActualFile(file));
	file.setUrl(createUrl(file));
	file.setLastModified(rs.getDate("timestamp"));

	if(!file.getActualFile().exists())
	{
	    log.error("Can't find file: " + file.getActualFile().getAbsolutePath());

	    file.setExists(false);

	    if(file.getMimeType().startsWith("image/"))
	    {
		file.setActualFile(new java.io.File(folder, "404-no-file.png"));
		file.setMimeType("image/png");
	    }
	    else
		file.setActualFile(null);
	}

	file = cache.put(file);
	return file;
    }

    private String createUrl(File file)
    {
	return baseUrl + file.getId() + "/" + StringHelper.urlEncode(file.getName());
    }

    private java.io.File createActualFile(File file)
    {
	return new java.io.File(folder, file.getId() + ".dat");
    }

    public void setContextUrl(String contextUrl)
    {
	this.baseUrl = contextUrl + urlPattern;
    }

    public static void main(String[] args) throws SQLException
    {
	Learnweb learnweb = Learnweb.getInstance();

	findAbandonedFiles();

	System.out.println("fertig");
	learnweb.onDestroy();
    }

    private static void findAbandonedFiles() throws SQLException
    {
	FileManager fm = Learnweb.getInstance().getFileManager();

	for(final java.io.File file : fm.folder.listFiles())
	{
	    //System.out.println(file.getName());

	    String[] splits = file.getName().split("\\.");

	    if(splits[1] == "dat")
	    {
		int id = Integer.parseInt(splits[0]);
		if(fm.getFileById(id) == null)
		{
		    System.err.println("abandoned");
		}

	    }
	}
    }
}
