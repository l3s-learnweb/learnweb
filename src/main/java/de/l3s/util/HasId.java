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
@FunctionalInterface
public interface HasId extends Argument {

    int getId();

    /**
     * Returns a list of all ids of the given list of objects.
     */
    static ArrayList<Integer> collectIds(Collection<? extends HasId> collection) {
        ArrayList<Integer> ids = new ArrayList<>(collection.size());
        for (HasId item : collection) {
            ids.add(item.getId());
        }
        return ids;
    }

    static Integer getIdOrDefault(HasId object, Integer def) {
        if (object == null || object.getId() == 0) {
            return def;
        }
        return object.getId();
    }

    @Override
    default void apply(final int position, final PreparedStatement statement, final StatementContext ctx) throws SQLException {
        statement.setInt(position, getId());
    }
}
