package de.l3s.util;

public interface ICache<E>
{

    public abstract E get(int id);

    /**
     * This method behaves different from the underlying map method.
     * If the map previously contained a mapping for the key, the old value is <b>not</b> replaced by the specified value.
     * 
     * @param id
     * @param resource
     * @return The mapping for the key (so maybe not the provided resource)
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
