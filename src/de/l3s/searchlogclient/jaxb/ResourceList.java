package de.l3s.searchlogclient.jaxb;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ResourceList")
public class ResourceList
{

    LinkedList<Resource> resources;

    public LinkedList<Resource> getResources()
    {
        return resources;
    }

    public void setResources(LinkedList<Resource> resources)
    {
        this.resources = resources;
    }

}
