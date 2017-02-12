package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.glossary.GlossaryItems;
import de.l3s.glossary.ItalianItem;
import de.l3s.glossary.LanguageItems;
import de.l3s.glossary.UkItem;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@ViewScoped
public class ViewGlossaryBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = -3927594222612462194L;
    private int resourceId;
    private List<GlossaryItems> items = new ArrayList<GlossaryItems>();

    private int italianRows;
    private int ukRows;
    private int rowSpan;

    public ViewGlossaryBean()
    {
        System.out.println("Constructing ViewGlossaryBean");
    }

    public int getRowSpan()
    {
        return rowSpan;
    }

    public void setRowSpan(int rowSpan)
    {
        this.rowSpan = rowSpan;
    }

    /* @PostConstruct
    public void init()
    {
        System.out.println(resourceId);
        getGlossaryItems(resourceId);
    
    }*/

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        System.out.println("Inside PreRenderView of ViewGlossaryBean: ResourceId ->" + resourceId);
        if(resourceId > 0)
        {
            getGlossaryItems(resourceId);
        }
    }

    private void getGlossaryItems(int id)
    {
        String mainDetails = "SELECT * FROM `lw_resource_glossary` WHERE `resource_id` = ?";
        String termDetails = "SELECT * FROM `lw_resource_glossary_terms` WHERE `glossary_id` = ?";
        PreparedStatement preparedStmnt = null;
        ResultSet result = null;
        List<ItalianItem> i = new ArrayList<ItalianItem>();
        List<UkItem> u = new ArrayList<UkItem>();
        try
        {
            preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(mainDetails);
            preparedStmnt.setInt(1, id);
            result = preparedStmnt.executeQuery();
            while(result.next())
            {
                List<LanguageItems> finalList = new ArrayList<LanguageItems>();
                GlossaryItems gloss = new GlossaryItems();
                gloss.setGlossId(result.getInt("glossary_id"));
                System.out.println(result.getInt("glossary_id"));
                gloss.setTopic_1(result.getString("topic_1"));
                gloss.setTopic_2(result.getString("topic_2"));
                gloss.setTopic_3(result.getString("topic_3"));
                gloss.setDescription(result.getString("description"));
                int glossaryId = result.getInt("glossary_id");
                PreparedStatement ps = Learnweb.getInstance().getConnection().prepareStatement(termDetails);
                ps.setInt(1, glossaryId);
                ResultSet termResults = ps.executeQuery();
                while(termResults.next())
                {

                    LanguageItems uk = new LanguageItems();
                    uk.setAcronym(termResults.getString("acronym"));
                    uk.setValue(termResults.getString("term"));
                    uk.setPhraseology(termResults.getString("phraseology"));
                    uk.setPronounciation(termResults.getString("pronounciation"));
                    uk.setReferences(termResults.getString("references"));
                    uk.setTermId(termResults.getInt("glossary_term_id"));
                    uk.setSelectedUses(termResults.getString("use"));
                    if(termResults.getString("language").contains("uk"))
                        uk.setLanguage("English");
                    else
                        uk.setLanguage("Italian");
                    finalList.add(uk);

                }
                //    setItalianRows(i.size());
                //   setUkRows(u.size());
                // gloss.setRowspan(ukRows > italianRows ? ukRows : italianRows);
                gloss.setRowspan(finalList.size() + 1);
                gloss.setFinalItems(finalList);
                items.add(gloss);

            }

        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public List<GlossaryItems> getItems()
    {
        return items;
    }

    public void setItems(List<GlossaryItems> items)
    {
        this.items = items;
    }

    public int getItalianRows()
    {
        return italianRows;
    }

    public void setItalianRows(int italianRows)
    {
        this.italianRows = italianRows;
    }

    public int getUkRows()
    {
        return ukRows;
    }

    public void setUkRows(int ukRows)
    {
        this.ukRows = ukRows;
    }
}
