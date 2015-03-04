package de.l3s.learnweb;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class can be used to store a list of elements. The list also stores who owns the element
 * 
 * @author Kemkes
 *
 * @param <E> an element
 * @param <O> the owner of this element
 */
public class OwnerList<E,O> extends LinkedList<E> 
{
	private static final long serialVersionUID = -2264077519939704399L;
	
	private HashMap<E, O> elementOwner = new HashMap<E, O>();
	private HashMap<E, Date> elementTimestamp = new HashMap<E, Date>();
	
	/**
	 * Copy constructor
	 * @param ol
	 */
	public OwnerList(OwnerList<E,O> ol)
	{
		super(ol);
		elementOwner = ol.elementOwner;
		elementTimestamp = ol.elementTimestamp;
	}
	
	public OwnerList()
	{
		super();
	}
	
	public boolean add(E e, O owner, Date date) 
	{
		elementOwner.put(e, owner);
		elementTimestamp.put(e, date);
		return add(e);
	}
	
	public O getElementOwner(E e)
	{
		return elementOwner.get(e);
	}
	
	public Date getElementTimestamp(E e)
	{
		return elementTimestamp.get(e);
	}
	
	@Override
	public boolean remove(Object o) 
	{
		elementOwner.remove(o);
		return super.remove(o);
	}
}
