package jcdashboard.model;

import java.util.ArrayList;
import java.util.List;

public class Albero
{
    String nome;

    //Collection of child graphics.
    private List<Albero> childGraphics = new ArrayList<Albero>();

    public void add(Albero graphic)
    {
        childGraphics.add(graphic);
    }

    public void remove(Albero graphic)
    {
        childGraphics.remove(graphic);
    }

    public Albero get(Albero a)
    {
        if(childGraphics.contains(a))
            return childGraphics.get(childGraphics.indexOf(a));
        else
            return null;
    }

}
