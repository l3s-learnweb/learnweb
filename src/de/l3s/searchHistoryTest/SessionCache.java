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
        userSessionCache = new HashMap<>();
        groupSessionCache = new HashMap<>();
    }

    private Map<Integer, List<Session>> userSessionCache = null;
    private Map<Integer, List<Session>> groupSessionCache = null;

    public synchronized void cacheByUserId(int userId, List<Session> sessions)
    {
        this.userSessionCache.put(userId, sessions);
    }

    public synchronized List<Session> getByUserId(int userId)
    {
        return this.userSessionCache.get(userId);
    }

    public synchronized boolean existsUserId(int userId)
    {
        return this.userSessionCache.containsKey(userId);
    }

    public synchronized void cacheByGroupId(int groupId, List<Session> sessions)
    {
        this.groupSessionCache.put(groupId, sessions);
    }

    public synchronized List<Session> getByGroupId(int groupId)
    {
        return this.groupSessionCache.get(groupId);
    }

    public synchronized boolean existsGroupId(int groupId)
    {
        return this.groupSessionCache.containsKey(groupId);
    }
}
