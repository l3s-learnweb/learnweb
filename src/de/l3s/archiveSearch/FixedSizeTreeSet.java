package de.l3s.archiveSearch;

import java.util.TreeSet;

public class FixedSizeTreeSet<E> extends TreeSet<E>
{
    private static final long serialVersionUID = 1823833240050570839L;
    private int maxSize;

    public FixedSizeTreeSet(int maxSize)
    {
	super();
	this.maxSize = maxSize;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean add(E e)
    {
	if(size() >= maxSize)
	{
	    E smallest = last();
	    int comparison = ((Comparable<E>) e).compareTo(smallest);

	    if(comparison <= 0) // defined for the view count; doesn't work for every use case
	    {
		pollLast();
		return super.add(e);
	    }
	    return false;
	}
	else
	{
	    return super.add(e);
	}
    }

}
