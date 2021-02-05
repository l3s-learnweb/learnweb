package de.l3s.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.github.benmanes.caffeine.cache.Caffeine;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;

/**
 * A synchronized cache that caches the defined number of most used objects.
 *
 * @author Philipp Kemkes
 */
public class Cache<E extends HasId> implements ICache<E> {
    private final int capacity;
    private final com.github.benmanes.caffeine.cache.Cache<Integer, E> weakValues; // cached values which are removed when they are not referenced else where
    private final Map<Integer, E> values; // makes sure to keep a reference to the X most recently added values, as defined by capacity

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public static <E extends HasId> ICache<E> of(Class<E> clazz) {
        Learnweb learnweb = Learnweb.getInstance();
        int resourceCacheSize = learnweb.getProperties().getPropertyIntValue("RESOURCE_CACHE");
        int groupCacheSize = learnweb.getProperties().getPropertyIntValue("GROUP_CACHE");
        int folderCacheSize = learnweb.getProperties().getPropertyIntValue("FOLDER_CACHE");
        int userCacheSize = learnweb.getProperties().getPropertyIntValue("USER_CACHE");

        if (clazz.equals(Group.class)) {
            return new Cache<>(groupCacheSize);
        } else if (clazz.equals(Folder.class)) {
            return new Cache<>(folderCacheSize);
        } else if (clazz.equals(User.class)) {
            return new Cache<>(userCacheSize);
        } else if (clazz.equals(Resource.class)) {
            return new Cache<>(resourceCacheSize);
        } else if (clazz.equals(Course.class)) {
            return new Cache<>(100);
        } else {
            return new DummyCache<>();
        }
    }

    /**
     * @param capacity Number of objects this cache will store
     */
    public Cache(int capacity) {
        this.capacity = capacity;

        values = new LinkedHashMap<>(Cache.this.capacity + 1, 0.75F, true) {
            private static final long serialVersionUID = -7231532816950321903L;

            @Override
            public boolean removeEldestEntry(Map.Entry<Integer, E> eldest) {
                return super.size() > Cache.this.capacity;
            }
        };

        weakValues = Caffeine.newBuilder().weakValues().build();
    }

    @Override
    public E get(int id) {
        readLock.lock();
        try {
            return weakValues.getIfPresent(id);
        } finally {
            readLock.unlock();
        }
    }

    public E put(int id, E resource) {
        writeLock.lock();
        try {
            E old = weakValues.getIfPresent(id);
            if (null != old) {
                return old;
            }

            values.put(id, resource);
            weakValues.put(id, resource);

            return resource;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E put(E resource) {
        int id = resource.getId();
        return put(id, resource);
    }

    @Override
    public void remove(int resourceId) {
        writeLock.lock();
        try {
            values.remove(resourceId);
            weakValues.invalidate(resourceId);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            values.clear();
            weakValues.invalidateAll();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int size() {
        return values.size();
    }

    public long sizeSecondaryCache() {
        return weakValues.estimatedSize();
    }

    @Override
    public Collection<E> getValues() {
        return values.values();
    }
}
