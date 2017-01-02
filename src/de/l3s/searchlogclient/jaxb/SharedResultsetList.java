package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "SharedResultsets")
public class SharedResultsetList
{

    ArrayList<SharedResultset> sharedResultsets;

    public SharedResultsetList()
    {
        sharedResultsets = new ArrayList<SharedResultset>();
    }

    public ArrayList<SharedResultset> getSharedResultsets()
    {
        return sharedResultsets;
    }

    public void setSharedResultsets(ArrayList<SharedResultset> sharedResultsets)
    {
        this.sharedResultsets = sharedResultsets;
    }
}
