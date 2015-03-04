package de.l3s.learnweb;

public class QuerySuggestion implements Comparable<QuerySuggestion> {
	private String source;
	private String query;
	private double distance;
	private int results;
	
	public QuerySuggestion(String query, double distance, String source, int results) {
		this.query = query;
		this.distance = distance;
		this.source = source;
		this.results = results;
	}
	
	public QuerySuggestion(String query, double distance)
	{
		this.query = query;
		this.distance = distance;
		this.source = "google";
		this.results = 0;
	}
	
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}

	@Override
	public int compareTo(QuerySuggestion o) {
		return (int)((o.distance - this.distance)*1000);
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return query;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getResults() {
		return results;
	}

	public void setResults(int results) {
		this.results = results;
	}
}
