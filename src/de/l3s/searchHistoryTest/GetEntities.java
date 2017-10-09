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

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@ManagedBean
@ViewScoped
public class GetEntities implements Serializable
{

    /**
     * to get the related entities of each queries
     */
    private static final long serialVersionUID = -4801439520135369515L;
    private static final Logger log = Logger.getLogger(GetEntities.class);

    //private List<String> entities = new ArrayList<String>();
    private List<String> Entities = new ArrayList<String>();

    public List<String> getEntities()
    {
        JSONParser parser = new JSONParser();

        try
        {
            Object obj = parser.parse(new FileReader("/Users/user/Desktop/cs_al-2.json"));
            JSONArray sessionsArray = (JSONArray) obj;

            for(int i = 0; i < sessionsArray.size(); i++)
            {
                JSONObject sessionObj = (JSONObject) sessionsArray.get(i);
                String session_id = (String) sessionObj.get("session_id");
                System.out.println("session_id:" + session_id);

                JSONArray queriesArray = (JSONArray) sessionObj.get("queries");
                if(queriesArray != null)
                {
                    for(int j = 0; j < queriesArray.size(); j++)
                    {
                        JSONObject queryObj = (JSONObject) queriesArray.get(j);

                        String query = (String) queryObj.get("query");
                        System.out.println("query:" + query);

                        String timestamp = (String) queryObj.get("timestamp");
                        //System.out.println("timestamp:"+timestamp);

                        JSONArray snippetsArray = (JSONArray) queryObj.get("snippets");
                        List<String> entities = new ArrayList<String>();

                        if(snippetsArray != null)
                        {

                            for(int k = 0; k < snippetsArray.size(); k++)
                            {
                                JSONObject snippetObj = (JSONObject) snippetsArray.get(k);
                                JSONArray entitiesArray = (JSONArray) snippetObj.get("entities");
                                if(entitiesArray != null)
                                {
                                    for(int m = 0; m < entitiesArray.size(); m++)
                                    {
                                        String entity = (String) entitiesArray.get(m);
                                        System.out.println("entity:" + entity);
                                        entities.add(entity);
                                        //queries.add(entities);
                                    }
                                    System.out.println("this is from GetEntities" + entities);
                                }
                                Entities.add(snippetObj.toString());
                            }
                        }

                    }
                }
            }

        }
        catch(FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            log.error("unhandled error", e);
        }
        catch(IOException e)
        {
            // TODO Auto-generated catch block
            log.error("unhandled error", e);
        }
        catch(ParseException e)
        {
            // TODO Auto-generated catch block
            log.error("unhandled error", e);
        }
        //System.out.println("this is from GetEntities"+entities);
        System.out.println("Entities:" + Entities);
        return Entities;

    }

}
