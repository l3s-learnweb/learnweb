package de.l3s.util;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

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

    @Override
    public Collection<E> getValues()
    {
        return values.values();
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException
    {
        Learnweb lw = Learnweb.createInstance(null);
        Cache<Integer, User> c = CacheBuilder.newBuilder()
                //.maximumSize(3)
                .weakValues()
                .removalListener(new RemovalListener<Integer, User>()
                {
                    @Override
                    public void onRemoval(RemovalNotification<Integer, User> arg0)
                    {
                        System.out.println("remove " + arg0.getValue());

                    }
                })
                .build();

        List<User> users = lw.getUserManager().getUsersByGroupId(896);

        int adminId = users.get(0).getId();
        for(User user : users)
            c.put(user.getId(), user);

        //        System.out.println(c.getIfPresent(adminId));
        users.clear();
        users = null;

        gc();
        Misc.sleep(9000);
        System.out.println(c.getIfPresent(adminId));

        System.out.println(c.size());
        lw.onDestroy();
    }

    public static void gc()
    {
        Object obj = new Object();
        WeakReference ref = new WeakReference<Object>(obj);
        obj = null;
        while(ref.get() != null)
        {
            System.gc();
        }
    }
}
