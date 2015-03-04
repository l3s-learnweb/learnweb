package de.l3s.learnwebBeans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;

import simpack.measure.external.simmetrics.Levenshtein;
import de.l3s.learnweb.QuerySuggestion;

@ManagedBean
public class AutoCompleteBean extends ApplicationBean
{

    public List<QuerySuggestion> getSuggestions(String query) throws SQLException
    {
	List<QuerySuggestion> results = new ArrayList<QuerySuggestion>();
	Connection dbCon = getLearnweb().getConnection();
	PreparedStatement ps = dbCon.prepareStatement("SELECT `params`, COUNT(*) cnt FROM `lw_user_log` WHERE `params` LIKE CONCAT(?,'%') GROUP BY `params` ORDER BY cnt DESC LIMIT 20");
	ps.setString(1, query);
	ResultSet rs = ps.executeQuery();
	while(rs.next())
	{
	    QuerySuggestion qs = new QuerySuggestion(rs.getString(1), 0, "learnweb", getResultCount(rs.getString(1)));
	    results.add(qs);
	}
	Collections.sort(results, new QSByResourcesComparer());
	if(results.size() > 3)
	    results.subList(3, results.size()).clear();
	if(results.isEmpty() && query.trim().contains(" ") && query.length() > 4)
	{
	    int i = 0;
	    String temp = query;
	    do
	    {
		List<QuerySuggestion> qs = new ArrayList<QuerySuggestion>();
		temp = temp.substring(0, temp.length() - 1);
		ps = dbCon.prepareStatement("SELECT `params`, COUNT(*) cnt FROM `lw_user_log` WHERE `params` LIKE CONCAT(?,'%') GROUP BY `params` ORDER BY cnt DESC LIMIT 10");
		ps.setString(1, temp);
		rs = ps.executeQuery();
		while(rs.next())
		{
		    Levenshtein lv = new Levenshtein(query, rs.getString(1));
		    if(lv.getSimilarity() >= 0.5)
		    {
			qs.add(new QuerySuggestion(rs.getString(1), lv.getSimilarity(), "learnweb", getResultCount(rs.getString(1))));
		    }
		}
		if(!qs.isEmpty())
		{
		    Collections.sort(qs);
		    int j = 0;
		    for(QuerySuggestion q : qs)
		    {
			results.add(q);
			j++;
			if(j > 4)
			    break;
		    }
		}
		i++;
	    }
	    while(results.isEmpty() && i < 3);
	    if(results.isEmpty())
	    {
		List<QuerySuggestion> qs = new ArrayList<QuerySuggestion>();
		temp = query.trim().substring(0, query.trim().lastIndexOf(' '));
		ps = dbCon.prepareStatement("SELECT `params`, COUNT(*) cnt FROM `lw_user_log` WHERE `params` LIKE CONCAT(?,'%') GROUP BY `params` ORDER BY cnt DESC");
		ps.setString(1, temp);
		rs = ps.executeQuery();
		while(rs.next())
		{
		    Levenshtein lv = new Levenshtein(query, rs.getString(1));
		    if(lv.getSimilarity() > 0.8)
		    {
			qs.add(new QuerySuggestion(rs.getString(1), lv.getSimilarity(), "learnweb", getResultCount(rs.getString(1))));
		    }
		}
		if(!qs.isEmpty())
		{
		    Collections.sort(qs);
		    int j = 0;
		    for(QuerySuggestion q : qs)
		    {
			results.add(q);
			j++;
			if(j > 4)
			    break;
		    }
		}
	    }
	}
	results.addAll(getGoogleSuggestions(query, results));
	return results;
    }

    public List<QuerySuggestion> getGoogleSuggestions(String query, List<QuerySuggestion> lwresults)
    {
	List<QuerySuggestion> results = new ArrayList<QuerySuggestion>();
	List<String> lwqueries = new ArrayList<String>();
	for(QuerySuggestion qs : lwresults)
	    lwqueries.add(qs.getQuery());
	try
	{
	    Connection dbCon = getLearnweb().getQuerydbConnection();
	    PreparedStatement ps;
	    if(query.length() <= 3 && !query.contains(" "))
		ps = dbCon.prepareStatement("SELECT `query` FROM `autoquery_aq_google_com` WHERE `parent` = (SELECT `idx` FROM `autoquery_aq_google_com` WHERE `query` = ?) AND `query` NOT LIKE '%\\_' ORDER BY `inserttime` DESC, `idx` ASC LIMIT 6");
	    else
		ps = dbCon.prepareStatement("SELECT `query` FROM `autoquery_aq_google_com` WHERE `query` LIKE CONCAT(?,'%') AND `query` NOT LIKE '%\\_' ORDER BY `idx` ASC LIMIT 20");
	    ps.setString(1, query);
	    ResultSet rs = ps.executeQuery();
	    int i = 0;
	    while(rs.next())
	    {
		if(!lwqueries.contains(rs.getString(1)))
		{
		    results.add(new QuerySuggestion(rs.getString(1), 0));
		    i++;
		    if(i == 3)
			break;
		}
	    }
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	}
	return results;
    }

    public List<QuerySuggestion> getCorrectSpellings(String query) throws SQLException
    {
	List<QuerySuggestion> results = new ArrayList<QuerySuggestion>();
	List<QuerySuggestion> suggestions = new ArrayList<QuerySuggestion>();
	Connection dbCon = getLearnweb().getQuerydbConnection();
	PreparedStatement ps = dbCon.prepareStatement("SELECT `term` FROM `google_unique_terms` WHERE `term` SOUNDS LIKE (?)");
	ps.setString(1, query);
	ResultSet rs = ps.executeQuery();
	while(rs.next())
	{
	    Levenshtein lv = new Levenshtein(query, rs.getString(1));
	    if(lv.getSimilarity() >= 0.5)
	    {
		suggestions.add(new QuerySuggestion(rs.getString(1), lv.getSimilarity()));
	    }
	}
	Collections.sort(suggestions);
	int i = 1;
	for(QuerySuggestion qs : suggestions)
	{
	    results.add(qs);
	    i++;
	    if(i > 5)
		break;
	}
	return results;
    }

    public List<QuerySuggestion> complete(String query)
    {
	List<QuerySuggestion> results = new ArrayList<QuerySuggestion>();
	try
	{
	    results = getSuggestions(query);
	    if(results.isEmpty())
	    {
		if(!query.trim().contains(" ") && query.endsWith(" ") && query.trim() != "")
		{
		    results = getCorrectSpellings(query.trim());
		}
	    }
	}
	catch(SQLException e1)
	{
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	return results;
    }

    public int getResultCount(String query) throws SQLException
    {
	Connection dbCon = getLearnweb().getConnection();
	PreparedStatement ps = dbCon.prepareStatement("SELECT COUNT(*) c FROM `lw_resource` WHERE `query` LIKE (?)");
	ps.setString(1, query);
	ResultSet rs = ps.executeQuery();
	rs.next();
	return rs.getInt(1);
    }

    private class QSByResourcesComparer implements Comparator<QuerySuggestion>
    {

	@Override
	public int compare(QuerySuggestion qs1, QuerySuggestion qs2)
	{
	    return qs2.getResults() - qs1.getResults();
	}

    }

}
