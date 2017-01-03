package de.l3s.util;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;

public class LRUCache<E> implements ICache<E>
{

    private Map<Integer, E> values;

    public LRUCache(int maxSize)
    {
        values = Collections.synchronizedMap(new LRUMap<Integer, E>(maxSize));
    }

    @Override
    public E get(int id)
    {
        return values.get(id);
    }

    private E put(int id, E resource)
    {
        synchronized(values)
        {
            E old = values.get(id);
            if(null != old)
                return old;

            values.put(id, resource);
            return resource;
        }
    }

    @Override
    public E put(E resource)
    {
        int id = ((HasId) resource).getId();
        return put(id, resource);
    }

    @Override
    public void remove(int resourceId)
    {
        values.remove(resourceId);
    }

    @Override
    public void clear()
    {
        values.clear();
    }

    @Override
    public int size()
    {
        return values.size();
    }
}
