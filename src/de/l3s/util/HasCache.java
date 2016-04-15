package de.l3s.util;

public interface HasCache
{
    public void resetCache();

    /**
     * 
     * @return Number of cached objects
     */
    public int getCacheSize();
}
