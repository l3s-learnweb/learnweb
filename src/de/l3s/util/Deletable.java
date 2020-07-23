package de.l3s.util;

/**
 * An interface which signalize that the object can have deleted state (soft delete).
 *
 * @author Oleh Astappiev
 */
public interface Deletable {
    boolean isDeleted();
}
