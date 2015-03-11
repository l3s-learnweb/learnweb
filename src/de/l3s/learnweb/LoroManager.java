package de.l3s.learnweb;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.l3s.learnweb.solrClient.FileInspector;
import de.l3s.learnweb.solrClient.SolrClient;

public class LoroManager
{
    private final static Logger log = Logger.getLogger(LoroManager.class);
    private final Learnweb learnweb;

    public LoroManager(Learnweb learnweb)
    {
	this.learnweb = learnweb;
    }

    //For saving Loro resources to LW table
    public void saveLoroResource() throws SQLException, IOException, SolrServerException
    {
	ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
	SolrClient solr = learnweb.getSolrClient();
	Group loroGroup = learnweb.getGroupManager().getGroupById(883);
	User admin = learnweb.getUserManager().getUser(7727);
	PreparedStatement update = Learnweb.getInstance().getConnection().prepareStatement("UPDATE LORO_resource SET resource_id = ? WHERE loro_resource_id = ?");

	PreparedStatement getLoroResource = Learnweb
		.getInstance()
		.getConnection()
		.prepareStatement(
			"SELECT t1.loro_resource_id , t1.description , t1.tags , t1.title , t1.creator_name , t1.course_code , t1.language_level , t1.languages , t1.preview_img_url,  t2.filename , t2.doc_format , t2.doc_url FROM LORO_resource t1 JOIN LORO_resource_docs t2 ON t1.loro_resource_id = t2.loro_resource_id");
	getLoroResource.executeQuery();
	ResultSet rs = getLoroResource.getResultSet();
	while(rs.next())
	{
	    int learnwebResourceId = rs.getInt("resource_id");

	    Resource loroResource = createResource(rs, learnwebResourceId);
	    int loroId = Integer.parseInt(loroResource.getIdAtService());

	    loroResource.setOwner(admin);

	    if(learnwebResourceId == 0) // not yet stored in Learnweb

	    {
		rpm.processImage(loroResource, FileInspector.openStream(loroResource.getMaxImageUrl()));
		loroResource.save();
		update.setInt(1, loroResource.getId());
		update.setInt(2, loroId);
		update.executeUpdate();

		admin.addResource(loroResource);
		loroGroup.addResource(loroResource, admin);

		solr.indexResource(loroResource);

	    }
	    else
		loroResource.save();

	    log.debug("Processed; lw: " + learnwebResourceId + " ted: " + loroId + " title:" + loroResource.getTitle());
	}

    }

    //Yet to be defined properly
    private Resource createResource(ResultSet rs, int learnwebResourceId) throws SQLException
    {
	Resource resource = new Resource();

	if(learnwebResourceId != 0) // the video is already stored and will be updated
	    resource = learnweb.getResourceManager().getResource(learnwebResourceId);

	resource.setTitle(rs.getString("title"));
	resource.setDescription(rs.getString("description"));
	resource.setUrl("http://www.ted.com/talks/" + rs.getString("slug"));
	resource.setSource("LORO");
	resource.setLocation("LORO");
	resource.setType("Video");
	resource.setDuration(rs.getInt("duration"));
	resource.setMaxImageUrl(rs.getString("photo2_url"));
	resource.setIdAtService(Integer.toString(rs.getInt("ted_id")));

	resource.setEmbeddedRaw("<iframe src=\"http://embed.ted.com/talks/" + rs.getString("slug") + ".html\" width=\"100%\" height=\"100%\" frameborder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>");
	resource.setTranscript("");

	return resource;
    }

    public static void main(String[] args) throws Exception
    {

	TedManager tm = Learnweb.getInstance().getTedManager();
	tm.saveTedResource();
    }

}
