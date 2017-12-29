package de.l3s.searchHistoryTest;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class SearchHistoryManager
{

    private final static Logger log = Logger.getLogger(SearchHistoryManager.class);
    private final Learnweb learnweb;

    public SearchHistoryManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public List<String> getQueriesForSessionId(String sessionId)
    {
        List<String> queries = new ArrayList<String>();
        return queries;
    }
}
