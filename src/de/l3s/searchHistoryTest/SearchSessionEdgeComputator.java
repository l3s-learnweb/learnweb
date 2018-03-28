package de.l3s.searchHistoryTest;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;

import de.l3s.learnweb.Learnweb;

public class SearchSessionEdgeComputator
{
    public final static Logger log = Logger.getLogger(SearchSessionEdgeComputator.class);

    private static SearchSessionEdgeComputator instance = null;

    private final String wikipediaSolrUrl;
    private final HttpSolrClient wikipediaSolrClient;

    private Learnweb learnweb;

    private SearchSessionEdgeComputator(Learnweb learnweb)
    {
        instance = this;
        this.wikipediaSolrUrl = learnweb.getProperties().getProperty("WIKIPEDIA_SOLR_URL");
        this.wikipediaSolrClient = new HttpSolrClient.Builder(wikipediaSolrUrl).build();
        this.learnweb = learnweb;
    }

    public static SearchSessionEdgeComputator getInstance(Learnweb learnweb)
    {
        if(null == instance)
            return new SearchSessionEdgeComputator(learnweb);

        return instance;
    }

    public HttpSolrClient getWikipediaSolrClient()
    {
        return wikipediaSolrClient;
    }

    public void insertEdgesForAllSessions() throws SQLException
    {
        PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
                "SELECT t2.session_id, t1.timestamp FROM learnweb_large.`sl_query` t1 join learnweb_main.lw_user_log t2 WHERE t1.search_id=t2.target_id AND t2.action = 5 AND t1.user_id=t2.user_id AND t1.query=t2.params AND t1.mode='text' AND t1.language='en' GROUP BY t2.session_id HAVING t1.timestamp > '2018-02-14' ORDER BY t1.timestamp ASC");
        ResultSet rs = pStmt.executeQuery();
        while(rs.next())
        {
            String sessionId = rs.getString("session_id");
            insertEdgesForSessionId(sessionId);
        }
        pStmt.close();
    }

    public String insertEdgesForSessionGivenSearchId(String searchId)
    {
        //log.info("insertEdgesForSessionGivenSearchId is applied.");
        try
        {
            PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
                    "SELECT t2.session_id FROM learnweb_large.`sl_query` t1 join learnweb_main.lw_user_log t2 "
                            + "WHERE t1.search_id=t2.target_id AND t2.action = 5 AND t1.user_id=t2.user_id AND t1.query=t2.params AND "
                            + "t1.mode='text' AND t1.language='en' AND t1.user_id != 0 AND t1.search_id=?");
            //+ "HAVING t1.timestamp >= (select timestamp from learnweb_large.sl_query where language='en' order by timestamp desc limit 1)"
            //+ "ORDER BY t1.timestamp ASC");
            pstmt.setString(1, searchId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                String sessionId = rs.getString("session_id");
                insertEdgesForSessionId(sessionId);
            }
            pstmt.close();
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving session id for search id: " + searchId, e);
        }
        return null;
    }

    public void insertEdgesForSessionId(String sessionId)
    {
        List<String> entities = new ArrayList<String>();
        try
        {
            PreparedStatement pStmt = learnweb.getConnection().prepareStatement(
                    "SELECT t1.search_id, t1.query, t1.timestamp FROM learnweb_large.`sl_query` t1 join learnweb_main.lw_user_log t2 WHERE t1.search_id=t2.target_id AND t2.action = 5 AND t1.user_id=t2.user_id AND t1.query=t2.params AND t1.mode='text' AND t1.language='en' AND t2.session_id=? AND t1.user_id != 0 ORDER BY t1.timestamp ASC");
            pStmt.setString(1, sessionId);
            ResultSet rs = pStmt.executeQuery();
            while(rs.next())
            {
                int searchId = rs.getInt("search_id");
                String query = rs.getString("query");
                entities.add(query);
                List<String> relatedEntities = getRelatedEntitiesForSearchId(searchId);
                //log.info("related: " + related);
                entities.addAll(relatedEntities);
                log.info("searchId: " + searchId + "; related entities size: " + relatedEntities.size());
            }
            pStmt.close();
            log.info("sessionId: " + sessionId + "; related entities size: " + entities.size());
        }
        catch(SQLException e)
        {
            log.error("Error while fetching queries for a specific session: " + sessionId, e);
        }
        //log.info("entities: " + entities);
        Map<EntityPair, Double> entityCooccurrences = getEntityPairScores(entities);
        insertEdgeScores(entityCooccurrences);
        log.info("No. of entity pairs inserted for session " + sessionId + " are: " + entityCooccurrences.size());
    }

    public List<String> getRelatedEntitiesForSearchId(int searchId)
    {
        List<Entity> relatedEntities = learnweb.getSearchHistoryManager().getRelatedEntitiesForSearchId(searchId);
        List<String> entityNames = relatedEntities.stream().map(e -> e.getEntityName()).collect(Collectors.toList());
        return entityNames;
    }

    public void insertEdgeScores(Map<EntityPair, Double> entityCooccurrences)
    {
        try
        {
            PreparedStatement insertStmt = learnweb.getConnection().prepareStatement("REPLACE INTO learnweb_large.sl_entity_co_occur(source, target, score) VALUES (?, ?, ?)");
            int batchCount = 0;
            int entityCooccurrencesSize = entityCooccurrences.size();
            for(Map.Entry<EntityPair, Double> entry : entityCooccurrences.entrySet())
            {
                EntityPair key = entry.getKey();
                insertStmt.setString(1, key.getEntity1());
                insertStmt.setString(2, key.getEntity2());
                insertStmt.setDouble(3, entry.getValue());
                insertStmt.addBatch();
                batchCount++;
                if(batchCount % 1000 == 0 || batchCount == entityCooccurrencesSize)
                    insertStmt.executeBatch();
            }
            insertStmt.close();
        }
        catch(SQLException e)
        {
            log.error("Error while inserting edge scores", e);
        }
    }

    public Map<EntityPair, Double> getEntityPairScores(List<String> entityList)
    {
        Map<EntityPair, Double> entityCooccurrence = new HashMap<EntityPair, Double>();
        try
        {
            Map<EntityPair, Double> unsortedEntityCooccur = getEdgeScore(entityList);
            List<Map.Entry<EntityPair, Double>> sortedEntityCooccur = sortByScore(unsortedEntityCooccur);
            //double normalizingConstant = getDenorminator(sortedEntityCooccur);
            double normalizingConstant = 1.0;

            for(Map.Entry<EntityPair, Double> entry : sortedEntityCooccur)
            {
                //log.info("entry.getValue():" + entry.getValue());
                double normalizedScore = entry.getValue() / normalizingConstant;
                entityCooccurrence.put(entry.getKey(), normalizedScore);
            }
        }
        catch(SolrServerException | IOException e)
        {
            log.error("Error while fetching scores from Solr", e);
        }
        return entityCooccurrence;
    }

    public Map<EntityPair, Double> getEdgeScore(List<String> entitylist) throws SolrServerException, IOException
    {
        Map<EntityPair, Double> entity_cooc = new HashMap<>();
        String q1, q2;

        SolrQuery query = new SolrQuery();
        for(int i = 0; i < entitylist.size(); i++)
        {
            for(int j = i + 1; j < entitylist.size(); j++)
            {
                q1 = entitylist.get(i);
                q2 = entitylist.get(j);
                //log.info("q1: " + q1 + "q2: " + q2);
                if(q1.toLowerCase().equals(q2.toLowerCase()))
                    continue;

                EntityPair key = new EntityPair(q1, q2);

                query.setQuery("REVISION_TEXT: " + "\"" + q1 + "\"" + " " + "&&" + " " + "REVISION_TEXT: " + "\"" + q2 + "\"");
                QueryResponse resp = getWikipediaSolrClient().query(query);
                long numFound = resp.getResults().getNumFound();
                //log.info("num of results returned: " + numFound);
                Double edgescore = (double) numFound;
                //Double edgescore = (double) numFound/12192531.0;

                entity_cooc.put(key, edgescore);
            }
        }
        return entity_cooc;
    }

    //sort entity pairs by score
    public static List<Map.Entry<EntityPair, Double>> sortByScore(Map<EntityPair, Double> m)
    {
        List<Map.Entry<EntityPair, Double>> sorted = new ArrayList<Map.Entry<EntityPair, Double>>(m.entrySet());
        Comparator<Map.Entry<EntityPair, Double>> valueComparator = new Comparator<Map.Entry<EntityPair, Double>>()
        {
            @Override
            public int compare(Map.Entry<EntityPair, Double> o1,
                    Map.Entry<EntityPair, Double> o2)
            {
                return (int) (o2.getValue() - o1.getValue());
            }
        };
        Collections.sort(sorted, valueComparator);
        return sorted;
    }

    //get denominator--the most popular occurred entity pair
    public static Double getDenorminator(List<Map.Entry<EntityPair, Double>> list)
    {
        double denorminator = 1.0;
        if(list.size() > 0)
        {
            Map.Entry<EntityPair, Double> en = list.get(0);
            denorminator = en.getValue();
            return denorminator;
        }
        return denorminator;
    }

    public static void main(String[] args) throws SolrServerException, IOException, ClassNotFoundException, SQLException
    {
        System.exit(0);
    }
}
