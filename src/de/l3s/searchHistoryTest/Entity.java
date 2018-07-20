package de.l3s.searchHistoryTest;

import java.util.ArrayList;
import java.util.List;

public class Entity
{
    private double score = 0.0;
    private String entityName = null;
    private String dbpediaName = null;
    private List<Integer> ranks = null;

    public Entity(String dbpediaName)
    {
        this.dbpediaName = dbpediaName;
        this.entityName = dbpediaName.replaceAll("[_()]", " ").trim();
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

    public String getDbpediaName()
    {
        return dbpediaName;
    }

    public void setDbpediaName(String dbpediaName)
    {
        this.dbpediaName = dbpediaName;
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
     * dbpediaName;score::3;1;5;6;2;...;4;9
     * 
     * @return
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(dbpediaName);
        builder.append(";").append(score);
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
        String[] entityInfoArr = tokens[0].split(";");
        Entity entity = new Entity(entityInfoArr[0]);
        entity.setScore(Double.parseDouble(entityInfoArr[1]));
        if(tokens.length == 2)
        {
            String[] rankStrs = tokens[1].split(";");
            for(String rankStr : rankStrs)
                entity.ranks.add(Integer.parseInt(rankStr));
        }
        return entity;
    }
}
