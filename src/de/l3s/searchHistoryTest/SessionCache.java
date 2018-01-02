package de.l3s.searchHistoryTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.l3s.searchHistoryTest.SearchHistoryManager.Session;

public class SessionCache
{
    private static SessionCache instance = null;

    public static SessionCache Instance()
    {
        if(instance == null)
        {
            instance = new SessionCache();
        }
        return instance;
    }

    private SessionCache()
    {
        cache = new HashMap<>();
    }

    private Map<Integer, List<Session>> cache = null;

    public synchronized void put(int userId, List<Session> sessions)
    {
        this.cache.put(userId, sessions);
    }

    public synchronized List<Session> get(int userId)
    {
        return this.cache.get(userId);
    }

    public synchronized boolean exists(int userId)
    {
        return this.cache.containsKey(userId);
    }

}
