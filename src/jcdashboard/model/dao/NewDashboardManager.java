package jcdashboard.model.dao;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnweb.UserManager;
import de.l3s.util.StringHelper;

/*
Used views:

resource:
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `resource` AS select `b`.`resource_id` AS `resource_id`,....
from (`learnweb_main`.`lw_user_course` `a` join `learnweb_main`.`lw_resource` `b` on((`a`.`user_id` = `b`.`owner_user_id`))) where (`a`.`course_id` = 1245)


 */
public class NewDashboardManager
{
    private static final Logger log = Logger.getLogger(NewDashboardManager.class);
    private static NewDashboardManager instance;

    private final Learnweb learnweb;

    public static NewDashboardManager getInstance(Learnweb learnweb)
    {
        if(instance == null)
            instance = new NewDashboardManager(learnweb);
        return instance;
    }

    private NewDashboardManager(Learnweb learnweb)
    {
        super();

        this.learnweb = learnweb;
    }

    public static void main(String[] args) throws ParseException, ClassNotFoundException, SQLException
    {
        String startDateStr = "2017-03-01";
        String endDateStr = "2017-06-01";
        Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(startDateStr);
        Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(endDateStr);
        List<Integer> userIds = Arrays.asList(10413, 10430, 10429, 10443, 10411, 10152, 10111, 10117, 10428, 10113);
        int userId = userIds.get(0);

        Learnweb learnweb = Learnweb.createInstance(null);
        NewDashboardManager newManager = getInstance(learnweb);
        UserLogHome oldManager = new UserLogHome();

        System.out.println(oldManager.actionPerDay());

        System.out.println("---------");

        System.out.println(oldManager.actionCount(startDateStr, endDateStr));
        System.out.println(newManager.getActionCountsPerAction(userIds, startDate, endDate));

        System.out.println("---------");

        System.out.println(oldManager.fields());
        System.out.println(newManager.getGlossaryFieldSummeryPerUser(userIds, startDate, endDate));

        oldManager.getTotalConcepts(startDateStr, endDateStr);
        oldManager.getTotalTerms(startDateStr, endDateStr);
        oldManager.getSummary2(startDateStr, endDateStr);

        oldManager.getTotalConcepts(startDateStr, endDateStr);
        oldManager.getTotalConcepts(startDateStr, endDateStr);

        oldManager.glossarySource(userId, startDateStr, endDateStr); //the new method should expect as parameters: int userId, Date startDate, Date endDate
    }

    public List<GlossaryFieldSummery> getGlossaryFieldSummeryPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        List<GlossaryFieldSummery> summeries = new ArrayList<>(userIds.size());

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT r.owner_user_id, "
                        + "COUNT(*) as count, COUNT( NULLIF( pronounciation, '' ) ) as pronounciation, "
                        + "COUNT( NULLIF( acronym, '' ) ) as acronym, "
                        + "COUNT( NULLIF( phraseology, '' ) ) as phraseology, "
                        + "COUNT( NULLIF( rgt.use, '' ) ) as uses, "
                        + "COUNT( NULLIF( rgt.references, '' ) ) as source "
                        + "FROM lw_resource r "
                        + "JOIN lw_resource_glossary rg USING(resource_id) "
                        + "JOIN lw_resource_glossary_terms rgt USING(glossary_id) "
                        + "WHERE rg.deleted != 1 AND r.deleted != 1 AND rgt.deleted != 1 "
                        + "AND owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND rg.timestamp BETWEEN ? AND ? GROUP BY r.owner_user_id");)
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                GlossaryFieldSummery fieldSummery = new GlossaryFieldSummery();
                fieldSummery.setUserId(rs.getInt("owner_user_id"));
                fieldSummery.setTotal(rs.getInt("count"));
                fieldSummery.setPronounciation(rs.getInt("pronounciation"));
                fieldSummery.setAcronym(rs.getInt("acronym"));
                fieldSummery.setPhraseology(rs.getInt("phraseology"));
                fieldSummery.setUses(rs.getInt("uses"));
                fieldSummery.setSource(rs.getInt("source"));

                summeries.add(fieldSummery);
            }
        }

        return summeries;
    }

    public Map<String, Integer> getUserGlossaryConceptCountByCourse(Collection<Integer> userIds, String startdate, String enddate) throws SQLException
    {
        Map<String, Integer> conceptsPerUser = new TreeMap<String, Integer>();

        UserManager userManager = learnweb.getUserManager();

        try(PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
                "SELECT user_id, count( * ) AS count FROM lw_resource r JOIN lw_user USING (user_id) JOIN lw_resource_glossary rg USING (resource_id) " +
                        "WHERE r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") AND rg.deleted != 1 AND r.deleted != 1 AND rg.timestamp > ? AND rg.timestamp < ? GROUP BY r.owner_user_id ORDER BY username");)
        {
            pstmt.setString(1, startdate);
            pstmt.setString(2, enddate);

            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                conceptsPerUser.put(userManager.getUser(rs.getInt("user_id")).getUsername(), rs.getInt("count"));
            }
        }
        return conceptsPerUser;
    }

    public Map<String, Integer> getActionCountsPerAction(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT action, COUNT(*) as count from lw_user_log where timestamp BETWEEN ? AND ? and user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") GROUP BY action");)
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();
            while(rs.next())
                actperday.put(rs.getString("action"), rs.getInt("count"));
        }

        return actperday;
    }

    /*
    public String getTopbar01data(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        int search = 0;
        int glossary = 0;
        int resource = 0;
        int system = 0;
        UserLogHome ulh = new UserLogHome();
        Map<String, Integer> mappa = getActionCounts(userIds, startDate, endDate);

        for(String k : mappa.keySet())
        {
            if(k.contains("search"))
                search += mappa.get(k);
            else if(k.contains("glossary"))
                glossary += mappa.get(k);
            else if(k.contains("resource"))
                resource += mappa.get(k);
            else
                system += mappa.get(k);
        }
        return topbar01data;
    }*/

    public static class GlossaryFieldSummery implements Serializable
    {
        private static final long serialVersionUID = -4378112533840640208L;
        int userId;
        int total;
        int pronounciation;
        int acronym;
        int phraseology;
        int uses;
        int source;

        private transient User user;

        public float getAvg()
        {
            return ((float) (pronounciation + acronym + phraseology + uses + source) / (total * 5));
        }

        public User getUser()
        {
            if(null == user)
            {
                try
                {
                    user = Learnweb.getInstance().getUserManager().getUser(userId);
                }
                catch(SQLException e)
                {
                    log.fatal("can't get user: " + userId, e);
                }
            }
            return user;
        }

        public int getUserid()
        {
            return userId;
        }

        private void setUserId(int userid)
        {
            this.userId = userid;
        }

        public int getTotal()
        {
            return total;
        }

        private void setTotal(int total)
        {
            this.total = total;
        }

        public int getPronounciation()
        {
            return pronounciation;
        }

        private void setPronounciation(int pronounciation)
        {
            this.pronounciation = pronounciation;
        }

        public int getAcronym()
        {
            return acronym;
        }

        private void setAcronym(int acronym)
        {
            this.acronym = acronym;
        }

        public int getPhraseology()
        {
            return phraseology;
        }

        private void setPhraseology(int phraseology)
        {
            this.phraseology = phraseology;
        }

        public int getUses()
        {
            return uses;
        }

        private void setUses(int uses)
        {
            this.uses = uses;
        }

        public int getSource()
        {
            return source;
        }

        private void setSource(int source)
        {
            this.source = source;
        }

        @Override
        public String toString()
        {
            return "GlossaryFieldSummery [userId=" + userId + ", total=" + total + ", pronounciation=" + pronounciation + ", acronym=" + acronym + ", phraseology=" + phraseology + ", uses=" + uses + ", source=" + source + "]";
        }

    }
}
