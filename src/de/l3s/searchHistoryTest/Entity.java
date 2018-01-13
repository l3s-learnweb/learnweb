package de.l3s.searchHistoryTest;

import java.util.ArrayList;
import java.util.List;

public class Entity
{
    private double score = 0.0;
    private String entityName = null;
    private List<Integer> ranks = null;

    public Entity(String entityName)
    {
        this.entityName = entityName;
        this.ranks = new ArrayList<>();
    }

    public Double getScore()
    {
        return this.score;
    }

    public void setScore(Double score)
    {
        this.score = score;
    }

    public String getEntityName()
    {
        return this.entityName;
    }

    public void setEntityName(String entityName)
    {
        this.entityName = entityName;
    }

    public void addRank(int rank)
    {
        if(!ranks.contains(rank))
        {
            this.ranks.add(rank);
        }
    }

    public List<Integer> getRanks()
    {
        return this.ranks;
    }

    @Override
    public int hashCode()
    {
        return this.entityName.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if(o instanceof String)
        {
            return this.entityName.equals(o);
        }
        if(o instanceof Entity)
        {
            Entity entity = (Entity) o;
            return this.entityName.equals(entity.entityName);
        }
        return false;
    }

    /**
     * The string format is:
     * entityName::3;1;5;6;2;...;4;9
     * 
     * @return
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(this.entityName);
        builder.append("::");
        boolean flag = false;
        for(int rank : this.ranks)
        {
            builder.append(rank).append(";");
            flag = true;
        }
        return flag == true ? builder.substring(0, builder.length() - 1) : builder.toString();// drop the last ';'
    }

    public static Entity fromString(String str)
    {
        String[] tokens = str.split("::");
        Entity entity = new Entity(tokens[0]);
        if(tokens.length == 2)
        {
            String[] rankStrs = tokens[1].split(";");
            for(String rankStr : rankStrs)
            {
                entity.ranks.add(Integer.parseInt(rankStr));
            }
        }
        return entity;
    }
    /*
     * public static void main(String[] args) throws Exception
    {
        EntitySet entitySet = new EntitySet();
    
        // add entities to set.
        entitySet.add(new Entity("e1"));
        entitySet.add(new Entity("e2"));
    
        // update their ranks
        entitySet.get("e1").addRank(1);
        entitySet.get("e2").addRank(2);
    
        // transform an entity set to string list, so that it can be easily serialized and then stored in database.
        List<String> strs = entitySet.toStringList();
    
        // database operations...
    
        // then, after we get a string list from database, we can transform the strings to entities.
        Entity entity = Entity.fromString(strs.get(1));
        System.out.println(entity);
    }
     */
}
