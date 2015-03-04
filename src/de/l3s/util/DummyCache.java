package de.l3s.util;


/**
 * A dummy implementation of the ICache Interface, which doesn't cache anything
 * 
 * @author Philipp
 *
 * @param <E>
 */
public class DummyCache<E> implements ICache<E> 	
{
	public DummyCache() 
	{
	}
	
	/* (non-Javadoc)
	 * @see de.l3s.util.ICache#get(int)
	 */
	@Override
	public E get(int id)
	{
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.l3s.util.ICache#put(int, E)
	 */
	@Override
	public E put(int id, E resource) 
	{
		return resource;
	}
	
	/* (non-Javadoc)
	 * @see de.l3s.util.ICache#put(E)
	 */	
	@Override
	public E put(E resource)
	{	
		return resource;
	}
	
	/* (non-Javadoc)
	 * @see de.l3s.util.ICache#remove(int)
	 */
	@Override
	public void remove(int resourceId)
	{

	}
	
	/* (non-Javadoc)
	 * @see de.l3s.util.ICache#clear()
	 */
	@Override
	public void clear()
	{

	}
}