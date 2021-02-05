package de.l3s.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * A simple interface that forces to implement a getId() method.
 *
 * @author Philipp Kemkes
 */
public interface HasId extends Argument {

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

    @Override
    default void apply(final int position, final PreparedStatement statement, final StatementContext ctx) throws SQLException {
        statement.setInt(position, getId());
    }
}
