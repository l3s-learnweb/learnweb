package de.l3s.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple interface that forces to implement a getId() method
 *
 * @author Philipp
 *
 */
public interface HasId
{

    public int getId();

    /**
     * Returns a list of all ids of the given list of objects
     *
     * @param list
     * @return
     */
    public static List<Integer> collectIds(List<? extends HasId> list)
    {
        List<Integer> ids = new ArrayList<>(list.size());

        for(HasId obj : list)
        {
            ids.add(obj.getId());
        }
        return ids;
    }

    public static String implodeIds(List<? extends HasId> list)
    {
        return StringHelper.implodeInt(collectIds(list), ",");
    }
}
