package de.l3s.learnweb.resource;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class can be used to store a list of elements. The list also stores who owns the element
 *
 * @author Philipp Kemkes
 * @param <E> an element
 * @param <O> the owner of this element
 */
public class OwnerList<E, O> extends LinkedList<E> {
    @Serial
    private static final long serialVersionUID = -2264077519939704399L;

    private HashMap<E, O> elementOwner = new HashMap<>();
    private HashMap<E, LocalDateTime> elementTimestamp = new HashMap<>();

    /**
     * Copy constructor.
     */
    public OwnerList(OwnerList<E, O> ol) {
        super(ol);
        elementOwner = ol.elementOwner;
        elementTimestamp = ol.elementTimestamp;
    }

    public OwnerList() {
    }

    public boolean add(E e, O owner, LocalDateTime date) {
        elementOwner.put(e, owner);
        elementTimestamp.put(e, date);
        return add(e);
    }

    public O getElementOwner(E e) {
        return elementOwner.get(e);
    }

    @Override
    public boolean remove(Object o) {
        elementOwner.remove(o);
        return super.remove(o);
    }
}
