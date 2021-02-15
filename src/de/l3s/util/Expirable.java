package de.l3s.util;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Objects;

import org.primefaces.util.SerializableSupplier;

/**
 * A wrapper which holds value for a duration and gets a new one if the value is expired.
 * If serialized and then deserialized, always gets a new value.
 */
public final class Expirable<T> implements Serializable {
    private static final long serialVersionUID = 6877022635730725601L;

    private final TemporalAmount duration;
    private final SerializableSupplier<T> supplier;

    private transient T value;
    private transient Instant expireAfter;

    public Expirable(TemporalAmount duration, SerializableSupplier<T> supplier) {
        this.duration = duration;
        this.supplier = supplier;
    }

    public T get() {
        if (isExpired()) {
            return updateValue(supplier.get());
        }

        return value;
    }

    public boolean isExpired() {
        return expireAfter == null || Instant.now().isAfter(expireAfter);
    }

    private T updateValue(T value) {
        this.value = value;
        this.expireAfter = Instant.now().plus(duration);
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Expirable<?> expirable = (Expirable<?>) o;
        return Objects.equals(duration, expirable.duration) && Objects.equals(supplier, expirable.supplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, supplier);
    }
}
