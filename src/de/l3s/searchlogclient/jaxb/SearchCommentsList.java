package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SearchCommentsList
{

    ArrayList<CommentonSearch> comments;

    public SearchCommentsList()
    {
        comments = new ArrayList<CommentonSearch>();
    }

    @XmlElement(name = "SearchComment")
    public ArrayList<CommentonSearch> getComments()
    {
        return comments;
    }

    public void setComments(ArrayList<CommentonSearch> comments)
    {
        this.comments = comments;
    }

}
