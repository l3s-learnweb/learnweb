package de.l3s.learnweb.dashboard;

import java.util.Map;

public class ActivityGraphData
{
    private String name;

    private Map<String, Integer> actionsPerDay;

    public Map<String, Integer> getActionsPerDay()
    {
        return actionsPerDay;
    }

    public void setActionsPerDay(Map<String, Integer> actionsPerDay)
    {
        this.actionsPerDay = actionsPerDay;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
