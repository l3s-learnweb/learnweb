package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class PresentationManager
{
    private final static Logger log = Logger.getLogger(PresentationManager.class);
    private final Learnweb learnweb;

    protected PresentationManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;
    }

    private Presentation createPresentation(ResultSet rs) throws SQLException
    {
        Presentation p = new Presentation();
        try
        {
            p.setGroupId(rs.getInt("group_id"));
            p.setOwnerId(rs.getInt("user_id"));
            p.setPresentationId(rs.getInt("presentation_id"));
            p.setPresentationName(rs.getString("presentation_name"));
            p.setCode(rs.getString("code"));
            Document doc = Jsoup.parse(rs.getString("code"));
            String title;
            try
            {
                title = doc.getElementById("presentation_title").html();
            }
            catch(NullPointerException e)
            {
                title = "Title Not Available";
            }
            p.setPresentationTitle(title);
            p.setDate(new Date(rs.getTimestamp("timestamp").getTime()));
            /*
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date date = new Date(rs.getTimestamp("timestamp").getTime());
            String time = sdf.format(date);
            p.setDate(time);
            */
        }
        catch(Exception e)
        {
            log.error("presenation error", e);
        }
        return p;
    }

    private List<Presentation> getPresentations(String query, String param1, int... params) throws SQLException
    {
        List<Presentation> presentations = new LinkedList<Presentation>();
        PreparedStatement select = learnweb.getConnection().prepareStatement(query);

        int i = 1;
        if(null != param1)
            select.setString(i++, param1);

        for(int param : params)
            select.setInt(i++, param);

        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            presentations.add(createPresentation(rs));
        }
        select.close();

        return presentations;
    }

    public List<Presentation> getPresentationsByGroupId(int groupId) throws SQLException
    {

        return getPresentations("SELECT * FROM lw_presentation WHERE group_id = ? AND deleted=0", null, groupId);
    }

    public List<Presentation> getPresentationsByUserId(int userId) throws SQLException
    {
        return getPresentations("SELECT * FROM lw_presentation WHERE user_id = ? AND deleted=0", null, userId);
    }

    public Presentation getPresentationsById(int presentationId) throws SQLException
    {
        List<Presentation> p = new LinkedList<Presentation>();
        p = getPresentations("SELECT * FROM lw_presentation WHERE presentation_id = ? AND deleted=0", null, presentationId);
        return p.get(0);
    }

    public Learnweb getLearnweb()
    {
        return learnweb;
    }

    public void deletePresentation(int presentationId) throws SQLException
    {
        // TODO Auto-generated method stub

    }

    public void addPresentation(Presentation presentation) throws SQLException
    {
        // TODO Auto-generated method stub

    }

}
