package de.l3s.searchHistoryTest;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
//import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@ManagedBean
@ViewScoped
public class GetQueries implements Serializable{

	/**
	 * to get queries from query logs
	 */
	private static final long serialVersionUID = 4275416428644103849L;

	private List<String> queries = new ArrayList<String>();
	public List<String> getQueries(){
		JSONParser parser = new JSONParser();
		
		try {
			Object obj = parser.parse(new FileReader("/Users/user/Desktop/cs_al-2.json"));
			JSONArray sessionsArray = (JSONArray) obj;
			
			for(int i=0;i<sessionsArray.size();i++){
				JSONObject sessionObj = (JSONObject) sessionsArray.get(i);
				String session_id = (String) sessionObj.get("session_id");
				System.out.println("session_id:"+session_id);
				
				JSONArray queriesArray = (JSONArray) sessionObj.get("queries");
				if(queriesArray != null){
					for(int j=0;j<queriesArray.size(); j++){
						JSONObject queryObj = (JSONObject) queriesArray.get(j);
						String query = (String) queryObj.get("query");
						System.out.println("query:"+query);
						queries.add(query);
						
						//String timestamp = (String) queryObj.get("timestamp");
						//System.out.println("timestamp:"+timestamp);
					}
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("queries:"+queries);
		return queries;
	}
	
}
