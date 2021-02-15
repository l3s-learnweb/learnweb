package de.l3s.util;

/**
 * An interface which signalize that the object can have deleted state (soft delete).
 *
 * @author Oleh Astappiev
 */
@FunctionalInterface
public interface Deletable {
    boolean isDeleted();
}
