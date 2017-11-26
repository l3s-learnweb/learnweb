package de.l3s.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A synchronized cache that caches the defined number of most used objects.
 * 
 * @author Philipp
 *
 * @param <E>
 */
public class Cache<E> implements ICache<E>
{
    private Map<Integer, E> values;
    private int capacity;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    /**
     * 
     * @param capacity Number of objects this cache will store
     */
    public Cache(int capacity2)
    {
        this.capacity = capacity2;

        values = new LinkedHashMap<Integer, E>(capacity + 1, .75F, true)
        {
            private static final long serialVersionUID = -7231532816950321903L;

            @Override
            public boolean removeEldestEntry(Map.Entry<Integer, E> eldest)
            {
                return size() > capacity;
            }
        };
    }

    /* (non-Javadoc)
     * @see de.l3s.util.ICache#get(int)
     */
    @Override
    public E get(int id)
    {
        readLock.lock();
        try
        {
            return values.get(id);
        }
        finally
        {
            readLock.unlock();
        }
    }

    public E put(int id, E resource)
    {
        writeLock.lock();
        try
        {
            E old = values.get(id);
            if(null != old)
                return old;

            values.put(id, resource);
            return resource;
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /* (non-Javadoc)
     * @see de.l3s.util.ICache#put(E)
     */
    @Override
    public E put(E resource)
    {
        int id = ((HasId) resource).getId();
        return put(id, resource);
    }

    /* (non-Javadoc)
     * @see de.l3s.util.ICache#remove(int)
     */
    @Override
    public void remove(int resourceId)
    {
        writeLock.lock();
        try
        {
            values.remove(resourceId);
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /* (non-Javadoc)
     * @see de.l3s.util.ICache#clear()
     */
    @Override
    public void clear()
    {
        writeLock.lock();
        try
        {
            values.clear();
        }
        finally
        {
            writeLock.unlock();
        }
    }

    @Override
    public int size()
    {
        return values.size();
    }

    @Override
    public Collection<E> getValues()
    {
        return values.values();
    }
}
