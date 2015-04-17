package de.l3s.util;

public interface ICache<E>
{

    public abstract E get(int id);

    /**
     * 
     * @param id
     * @param resource
     * @return The same resource
     */
    public abstract E put(int id, E resource);

    /**
     * @param resource Has to implement the interface HasId.
     * @return The same resource
     */
    public abstract E put(E resource);

    public abstract void remove(int resourceId);

    public abstract void clear();

}
