package de.l3s.util;

import java.util.Collection;

/**
 * A dummy implementation of the ICache Interface, which doesn't cache anything.
 *
 * @author Philipp
 */
public class DummyCache<E> implements ICache<E> {
    public DummyCache() {}

    /* (non-Javadoc)
     * @see de.l3s.util.ICache#get(int)
     */
    @Override
    public E get(int id) {
        return null;
    }

    /* @Override
    public E put(int id, E resource) {
        return resource;
    }*/

    /* (non-Javadoc)
     * @see de.l3s.util.ICache#put(E)
     */
    @Override
    public E put(E resource) {
        return resource;
    }

    /* (non-Javadoc)
     * @see de.l3s.util.ICache#remove(int)
     */
    @Override
    public void remove(int resourceId) {

    }

    /* (non-Javadoc)
     * @see de.l3s.util.ICache#clear()
     */
    @Override
    public void clear() {

    }

    @Override
    public int size() {
        return -1;
    }

    @Override
    public Collection<E> getValues() {
        return null;
    }
}
