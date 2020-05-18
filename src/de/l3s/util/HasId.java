package de.l3s.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A simple interface that forces to implement a getId() method.
 *
 * @author Philipp
 */
public interface HasId {

    int getId();

    /**
     * Returns a list of all ids of the given list of objects.
     */
    static ArrayList<Integer> collectIds(Collection<? extends HasId> list) {
        ArrayList<Integer> ids = new ArrayList<>(list.size());

        for (HasId obj : list) {
            ids.add(obj.getId());
        }
        return ids;
    }

    static String implodeIds(Collection<? extends HasId> list) {
        StringBuilder out = new StringBuilder();
        for (HasId item : list) {
            if (out.length() != 0) {
                out.append(',');
            }
            out.append(item.getId());
        }
        return out.toString();
    }

    static int getIdOrDefault(HasId object, Integer def) {
        if (object == null || object.getId() <= 0) {
            return def;
        }
        return object.getId();
    }
}
