package de.l3s.learnweb;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.primefaces.model.UploadedFile;

import de.l3s.glossary.ItalianItem;
import de.l3s.glossary.UkItem;
import de.l3s.learnweb.solrClient.SolrClient;

public class GlossariesManager
{
    private List<ItalianItem> ItalianItems;
    private List<UkItem> UkItems;
    private String fileName;
    private String selectedTopicOne;
    private String selectedTopicTwo;
    private String selectedTopicThree;
    public String description;
    private UploadedFile multimediaFile;
    private int resourceId;
    private int userId;
    private Learnweb learnweb;

    // cached values
    private transient User user;
    int glossaryId = 0;

    public GlossariesManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public void InsertTerms(int idGlossary)
    {
        String InsertTerms = "INSERT INTO `lw_resource_glossary_terms`(`glossary_id`, `term`, `use`, `pronounciation`, `acronym`, `references`, `phraseology`, `language`) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStmnt = null;
        try
        {
            preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(InsertTerms);
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try
        {
            for(UkItem t : UkItems)
            {
                preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(InsertTerms);
                preparedStmnt.setInt(1, idGlossary);
                preparedStmnt.setString(2, t.getValue());
                preparedStmnt.setString(3, String.join(", ", t.getSelectedUses()));
                preparedStmnt.setString(4, t.getPronounciation());
                preparedStmnt.setString(5, t.getAcronym());
                preparedStmnt.setString(6, t.getReferences());
                preparedStmnt.setString(7, t.getPhraseology());
                preparedStmnt.setString(8, "English-uk");
                preparedStmnt.executeQuery();

            }
            for(ItalianItem t : ItalianItems)
            {
                preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(InsertTerms);
                preparedStmnt.setInt(1, idGlossary);
                preparedStmnt.setString(2, t.getValue());
                preparedStmnt.setString(3, String.join(", ", t.getSelectedUses()));
                preparedStmnt.setString(4, t.getPronounciation());
                preparedStmnt.setString(5, t.getAcronym());
                preparedStmnt.setString(6, t.getReferences());
                preparedStmnt.setString(7, t.getPhraseology());
                preparedStmnt.setString(8, "Italian-it");
                preparedStmnt.executeQuery();

            }
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void addToDatabase(int i)
    {
        if(i == 0)
        {
            String InsertGlossary = "INSERT INTO `lw_resource_glossary`(`resource_id`, `topic_1`, `topic_2`, `topic_3`, `description`) VALUES ( ?, ?, ?, ?, ?)";

            PreparedStatement preparedStmnt = null;

            try
            {
                preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(InsertGlossary, PreparedStatement.RETURN_GENERATED_KEYS);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try
            {
                preparedStmnt.setInt(1, resourceId);
                preparedStmnt.setString(2, selectedTopicOne);
                preparedStmnt.setString(3, selectedTopicTwo);
                preparedStmnt.setString(4, selectedTopicThree);
                preparedStmnt.setString(5, description);
                preparedStmnt.executeQuery();
                ResultSet keys = preparedStmnt.getGeneratedKeys();
                keys.next();
                glossaryId = keys.getInt(1);
                InsertTerms(glossaryId);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            //            ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
            SolrClient solr = learnweb.getSolrClient();
            ResourceManager resourceManager = learnweb.getResourceManager();
            //           int learnwebResourceId = 0;
            try
            {
                Resource glossItem = resourceManager.getResource(resourceId);
                glossItem.setOwner(getUser());
                //  glossItem.setGroup(Learnweb.getInstance().getGroupManager().);
                /* 
                Image im = new Image(ImageIO.read(multimediaFile.getInputstream()));
                rpm.createThumbnails(glossItem, im, false);*/
                user.addResource(glossItem);
                //                learnwebResourceId = glossItem.getId();
                // glossItem.setType("Glossary");
                // glossItem.setDescription("");
                //glossItem.setType("Glossary");
                // glossItem.setIdAtService(Integer.toString(glossaryId));
                if(glossItem.getUrlReal().isEmpty())
                    glossItem.setUrl("/lw/glossary/showGlossary.jsf?resource_id=" + Integer.toString(glossItem.getId()));
                solr.indexResource(glossItem);
                glossItem.save();
            }
            catch(SQLException | IOException | SolrServerException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            /*String update = "UPDATE `lw_resource_glossary` SET `resource_id`= ? WHERE glossary_id = ?";
            preparedStmnt = null;
            try
            {
            preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(update);
            }
            catch(SQLException e)
            {
            // TODO Auto-generated catch block
            e.printStackTrace();
            }
            try
            {
            preparedStmnt.setInt(1, learnwebResourceId);
            preparedStmnt.setInt(2, glossaryId);
            preparedStmnt.executeUpdate();
            }
            catch(SQLException e)
            {
            // TODO Auto-generated catch block
            e.printStackTrace();
            }*/

        }
        else if(i > 0)
        {
            int glossIden = i;
            String updateStmnt = "UPDATE `lw_resource_glossary` SET `topic_1`= ? ,`topic_2`= ?,`topic_3`= ?,`description`= ? WHERE glossary_id = " + Integer.toString(glossIden);
            try
            {
                PreparedStatement pstmnt = Learnweb.getInstance().getConnection().prepareStatement(updateStmnt);
                pstmnt.setString(1, getSelectedTopicOne());
                pstmnt.setString(2, getSelectedTopicTwo());
                pstmnt.setString(3, getSelectedTopicThree());
                pstmnt.setString(4, getDescription());
                pstmnt.executeQuery();
                String deleteTerms = "SELECT glossary_term_id FROM lw_resource_glossary_terms WHERE glossary_id = " + Integer.toString(glossIden);
                String updateTerms = "UPDATE `lw_resource_glossary_terms` SET `term`= ? ,`use`= ? ,`pronounciation`= ? ,`acronym`= ? ,`references`= ? ,`phraseology`= ? ,`language`= ?  WHERE `glossary_term_id` = ?";
                List<UkItem> newUkItems = new ArrayList<UkItem>(getUkItems());

                List<ItalianItem> newItItems = new ArrayList<ItalianItem>(getItalianItems());
                pstmnt = null;
                pstmnt = Learnweb.getInstance().getConnection().prepareStatement(deleteTerms);
                ResultSet rs = pstmnt.executeQuery();
                while(rs.next())
                {
                    boolean deleteTerm = false;
                    for(UkItem u : UkItems)
                    {
                        if(rs.getInt("glossary_term_id") == u.getTermId())
                        {
                            deleteTerm = true;
                            break;
                        }
                    }
                    if(deleteTerm == false)
                    {
                        for(ItalianItem iItems : ItalianItems)
                        {
                            if(rs.getInt("glossary_term_id") == iItems.getTermId())
                            {
                                deleteTerm = true;
                                break;
                            }
                        }
                    }
                    if(deleteTerm != true)
                    {
                        String delete = "DELETE FROM `lw_resource_glossary_terms` WHERE glossary_term_id = " + Integer.toString(rs.getInt("glossary_term_id"));
                        PreparedStatement pd = Learnweb.getInstance().getConnection().prepareStatement(delete);
                        pd.executeUpdate();
                    }
                }

                for(UkItem u : getUkItems())
                {
                    PreparedStatement preparedStmnt = null;
                    if(u.getTermId() > 0)
                    {
                        preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(updateTerms);

                        preparedStmnt.setString(1, u.getValue());
                        String use = String.join(", ", u.getSelectedUses());
                        preparedStmnt.setString(2, use);
                        preparedStmnt.setString(3, u.getPronounciation());
                        preparedStmnt.setString(4, u.getAcronym());
                        preparedStmnt.setString(5, u.getReferences());
                        preparedStmnt.setString(6, u.getPhraseology());
                        preparedStmnt.setString(7, "English-uk");
                        preparedStmnt.setInt(8, u.getTermId());
                        preparedStmnt.executeQuery();

                        newUkItems.remove(u);

                    }
                }
                for(ItalianItem u : getItalianItems())
                {
                    PreparedStatement preparedStmnt = null;
                    if(u.getTermId() > 0)
                    {
                        preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(updateTerms);

                        preparedStmnt.setString(1, u.getValue());
                        String use = String.join(", ", u.getSelectedUses());
                        preparedStmnt.setString(2, use);
                        preparedStmnt.setString(3, u.getPronounciation());
                        preparedStmnt.setString(4, u.getAcronym());
                        preparedStmnt.setString(5, u.getReferences());
                        preparedStmnt.setString(6, u.getPhraseology());
                        preparedStmnt.setString(7, "Italian-it");
                        preparedStmnt.setInt(8, u.getTermId());
                        preparedStmnt.executeQuery();
                        newItItems.remove(u);

                    }
                }
                setItalianItems(newItItems);
                setUkItems(newUkItems);
                InsertTerms(glossIden);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        if(description != null)
            this.description = description;
        else
            this.description = "";

    }

    public List<ItalianItem> getItalianItems()
    {
        return ItalianItems;

    }

    public void setItalianItems(List<ItalianItem> itItems)
    {
        this.ItalianItems = itItems;

    }

    public List<UkItem> getUkItems()
    {

        return UkItems;

    }

    public void setUkItems(List<UkItem> ukItems)
    {

        this.UkItems = ukItems;

    }

    public String getSelectedTopicTwo()
    {
        return selectedTopicTwo;
    }

    public void setSelectedTopicTwo(String selectedTopicTwo)
    {
        if(selectedTopicTwo != null)
            this.selectedTopicTwo = selectedTopicTwo;
        else
            this.selectedTopicTwo = "";
    }

    public String getSelectedTopicOne()
    {
        return selectedTopicOne;
    }

    public void setSelectedTopicOne(String selectedTopicOne)
    {
        if(selectedTopicOne != null)
            this.selectedTopicOne = selectedTopicOne;
        else
            this.selectedTopicOne = "";
    }

    public String getSelectedTopicThree()
    {
        return selectedTopicThree;
    }

    public void setSelectedTopicThree(String selectedTopicThree)
    {
        if(selectedTopicThree != null)
            this.selectedTopicThree = selectedTopicThree;
        else
            this.selectedTopicThree = "";
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public UploadedFile getMultimediaFile()
    {

        return multimediaFile;

    }

    public void setMultimediaFile(UploadedFile multimediaFile)
    {
        this.multimediaFile = multimediaFile;
    }

    public User getUser()
    {
        if(user == null)
        {
            try
            {
                user = Learnweb.getInstance().getUserManager().getUser(userId);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;
        this.userId = user.getId();
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
        this.user = null;
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public void deleteFromDb(String deleteTerms, String deleteGlossItem)
    {
        try
        {
            PreparedStatement Terms = Learnweb.getInstance().getConnection().prepareStatement(deleteTerms);
            Terms.executeUpdate();
            PreparedStatement gloss = Learnweb.getInstance().getConnection().prepareStatement(deleteGlossItem);
            gloss.executeUpdate();
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /* private int glossaryId; // auto incremented id
    private int resourceId;
    private int userId;
    
    @NotEmpty
    private String item;
    private String description;
    private String topic;
    private String italian;
    private String german;
    private String spanish;
    private Date lastModified;
    
    // cached values
    private transient User user;
    
    @URL
    private String reference;
    
    public Date getLastModified()
    {
        return lastModified;
    }
    
    public void setLastModified(Date lastModified)
    {
        this.lastModified = lastModified;
    }
    
    public String getReference()
    {
        return reference;
    }
    
    public void setReference(String reference)
    {
        this.reference = reference;
    }
    
    public String getTopic()
    {
        return topic;
    }
    
    public void setTopic(String topic)
    {
        this.topic = topic;
    }
    
    public String getItalian()
    {
        return italian;
    }
    
    public void setItalian(String italian)
    {
        this.italian = italian;
    }
    
    public String getGerman()
    {
        return german;
    }
    
    public void setGerman(String german)
    {
        this.german = german;
    }
    
    public String getSpanish()
    {
        return spanish;
    }
    
    public void setSpanish(String spanish)
    {
        this.spanish = spanish;
    }
    
    public String getItem()
    {
        return item;
    }
    
    public void setItem(String item)
    {
        this.item = item;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public User getUser() throws SQLException
    {
        if(user == null)
        {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }
    
    public void setUser(User user)
    {
        this.user = user;
        this.userId = user.getId();
    }
    
    public int getId()
    {
        return glossaryId;
    }
    
    public void setId(int glossaryId)
    {
        this.glossaryId = glossaryId;
    }
    
    public int getResourceId()
    {
        return resourceId;
    }
    
    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }
    
    public int getUserId()
    {
        return userId;
    }
    
    public void setUserId(int userId)
    {
        this.userId = userId;
        this.user = null;
    }
    */
}
