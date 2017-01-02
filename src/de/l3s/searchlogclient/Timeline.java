package de.l3s.searchlogclient;

import java.util.ArrayList;

public class Timeline
{

    private String date;
    private ArrayList<String> timestamps;

    public Timeline()
    {
        date = "";
        timestamps = new ArrayList<String>();
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public ArrayList<String> getTimestamps()
    {
        return timestamps;
    }

    public void setTimestamps(ArrayList<String> timestamps)
    {
        this.timestamps = timestamps;
    }

}
