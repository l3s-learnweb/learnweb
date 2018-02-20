package de.l3s.learnweb.dashboard;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.StringHelper;

/*
Used views:

resource:
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `resource` AS select `b`.`resource_id` AS `resource_id`,....
from (`learnweb_main`.`lw_user_course` `a` join `learnweb_main`.`lw_resource` `b` on((`a`.`user_id` = `b`.`owner_user_id`))) where (`a`.`course_id` = 1245)


 */
public class DashboardManager
{
    private static final Logger log = Logger.getLogger(DashboardManager.class);
    private static DashboardManager instance;

    private final Learnweb learnweb;

    public static DashboardManager getInstance(Learnweb learnweb)
    {
        if(instance == null)
            instance = new DashboardManager(learnweb);
        return instance;
    }

    private DashboardManager(Learnweb learnweb)
    {
        super();

        this.learnweb = learnweb;
    }

    public List<GlossaryFieldSummery> getGlossaryFieldSummeryPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        List<GlossaryFieldSummery> summeries = new ArrayList<>(userIds.size());

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT r.owner_user_id, "
                        + "COUNT(*) as count, COUNT( NULLIF( pronounciation, '' ) ) as pronounciation,  "
                        + "COUNT( NULLIF( acronym, '' ) ) as acronym, "
                        + "COUNT( NULLIF( phraseology, '' ) ) as phraseology, "
                        + "COUNT( NULLIF( rgt.use, '' ) ) as uses, "
                        + "COUNT( NULLIF( rgt.references, '' ) ) as source "
                        + "FROM lw_resource r" + "JOIN lw_resource_glossary rg USING(resource_id) "
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
                fieldSummery.setUserid(rs.getInt("owner_user_id"));
                fieldSummery.setTotal(rs.getInt("count"));
                fieldSummery.setPronounciation(rs.getInt("pronounciation"));
                fieldSummery.setAcronym(rs.getInt("acronym"));
                fieldSummery.setPhraseology(rs.getInt("phraseology"));
                fieldSummery.setUses(rs.getInt("uses"));
                fieldSummery.setSource(rs.getInt("source"));
            }
        }

        return summeries;
    }

    public static class GlossaryFieldSummery
    {
        int userid;
        int total;
        int pronounciation;
        int acronym;
        int phraseology;
        int uses;
        int source;

        public float getAvg()
        {
            return ((float) (pronounciation + acronym + phraseology + uses + source) / (total * 5));
        }

        public int getUserid()
        {
            return userid;
        }

        public void setUserid(int userid)
        {
            this.userid = userid;
        }

        public int getTotal()
        {
            return total;
        }

        public void setTotal(int total)
        {
            this.total = total;
        }

        public int getPronounciation()
        {
            return pronounciation;
        }

        public void setPronounciation(int pronounciation)
        {
            this.pronounciation = pronounciation;
        }

        public int getAcronym()
        {
            return acronym;
        }

        public void setAcronym(int acronym)
        {
            this.acronym = acronym;
        }

        public int getPhraseology()
        {
            return phraseology;
        }

        public void setPhraseology(int phraseology)
        {
            this.phraseology = phraseology;
        }

        public int getUses()
        {
            return uses;
        }

        public void setUses(int uses)
        {
            this.uses = uses;
        }

        public int getSource()
        {
            return source;
        }

        public void setSource(int source)
        {
            this.source = source;
        }
    }
}
