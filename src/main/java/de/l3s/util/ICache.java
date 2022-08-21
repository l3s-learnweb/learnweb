package de.l3s.util;

import java.util.Collection;

public interface ICache<E> {

    E get(int id);

    /**
     * This method behaves different from the underlying map method.
     * If the map previously contained a mapping for the key, the old value is <b>not</b> replaced by the specified value.
     *
     * @param resource Has to implement the interface HasId.
     * @return The mapping for the key (so maybe not the provided resource)
     */
    E put(E resource);

    void remove(int resourceId);

    void clear();

    /**
     * @return number of cached objects
     */
    int size();

    long sizeSecondaryCache();

    /**
     * Return all values of this cache.
     */
    Collection<E> getValues();
}
