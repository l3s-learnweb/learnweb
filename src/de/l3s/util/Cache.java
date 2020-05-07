package de.l3s.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.cache.CacheBuilder;

/**
 * A synchronized cache that caches the defined number of most used objects.
 *
 * @author Philipp
 *
 * @param <E>
 */
public class Cache<E extends HasId> implements ICache<E>
{
    private int capacity;
    private com.google.common.cache.Cache<Integer, E> weakValues; // cached values which are removed when they are not referenced else where
    private Map<Integer, E> values; // makes sure to keep a reference to the X most recently added values, as defined by capacity

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    /**
     *
     * @param capacity2 Number of objects this cache will store
     */
    public Cache(int capacity2)
    {
        this.capacity = capacity2;

        values = new LinkedHashMap<>(capacity + 1, 0.75F, true)
        {
            private static final long serialVersionUID = -7231532816950321903L;

            @Override
            public boolean removeEldestEntry(Map.Entry<Integer, E> eldest)
            {
                return super.size() > capacity;
            }
        };

        weakValues = CacheBuilder.newBuilder()
                .weakValues()
                .build();
    }

    @Override
    public E get(int id)
    {
        readLock.lock();
        try
        {
            return weakValues.getIfPresent(id);
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
            E old = weakValues.getIfPresent(id);
            if(null != old)
                return old;

            values.put(id, resource);
            weakValues.put(id, resource);

            return resource;
        }
        finally
        {
            writeLock.unlock();
        }
    }

    @Override
    public E put(E resource)
    {
        int id = resource.getId();
        return put(id, resource);
    }

    @Override
    public void remove(int resourceId)
    {
        writeLock.lock();
        try
        {
            values.remove(resourceId);
            weakValues.invalidate(resourceId);
        }
        finally
        {
            writeLock.unlock();
        }
    }

    @Override
    public void clear()
    {
        writeLock.lock();
        try
        {
            values.clear();
            weakValues.invalidateAll();
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

    public long sizeSecondaryCache()
    {
        return weakValues.size();
    }

    @Override
    public Collection<E> getValues()
    {
        return values.values();
    }
}
