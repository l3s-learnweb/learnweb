package de.l3s.learnweb.searchhistory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.searchhistory.SearchHistoryManager.Session;

public class SessionCache
{
    private static SessionCache instance = null;

    private Map<Integer, List<Session>> userSessionCache;
    private Map<Integer, List<Session>> groupSessionCache;

    public static SessionCache instance()
    {
        if(instance == null)
        {
            instance = new SessionCache();
        }
        return instance;
    }

    private SessionCache()
    {
        userSessionCache = new HashMap<>();
        groupSessionCache = new HashMap<>();
    }

    public synchronized void cacheByUserId(int userId, List<Session> sessions)
    {
        userSessionCache.put(userId, sessions);
    }

    public synchronized List<Session> getByUserId(int userId)
    {
        return userSessionCache.get(userId);
    }

    public synchronized boolean existsUserId(int userId)
    {
        return userSessionCache.containsKey(userId);
    }

    public synchronized void cacheByGroupId(int groupId, List<Session> sessions)
    {
        groupSessionCache.put(groupId, sessions);
    }

    public synchronized List<Session> getByGroupId(int groupId)
    {
        return groupSessionCache.get(groupId);
    }

    public synchronized boolean existsGroupId(int groupId)
    {
        return groupSessionCache.containsKey(groupId);
    }
}
