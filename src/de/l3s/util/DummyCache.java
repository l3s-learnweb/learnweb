package de.l3s.util;

import java.util.Collection;

/**
 * A dummy implementation of the ICache Interface, which doesn't cache anything.
 *
 * @author Philipp Kemkes
 */
public class DummyCache<E> implements ICache<E> {
    @Override
    public E get(int id) {
        return null;
    }

    @Override
    public E put(E resource) {
        return resource;
    }

    @Override
    public void remove(int resourceId) {

    }

    @Override
    public void clear() {

    }

    @Override
    public int size() {
        return -1;
    }

    @Override
    public long sizeSecondaryCache() {
        return -1;
    }

    @Override
    public Collection<E> getValues() {
        return null;
    }
}
