package de.l3s.util;

import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.function.Supplier;

public final class Expirable<T> {

    private T value;
    private Instant expireAfter;

    private final TemporalAmount duration;
    private Supplier<T> supplier;

    public Expirable(T value, TemporalAmount duration) {
        this.duration = duration;
        updateValue(value);
    }

    public Expirable(TemporalAmount duration, Supplier<T> supplier) {
        this.duration = duration;
        this.supplier = supplier;
    }

    public T get() {
        if (isExpired()) {
            if (supplier != null) {
                return updateValue(supplier.get());
            }
            return null;
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
        return Objects.equals(value, expirable.value) && Objects.equals(expireAfter, expirable.expireAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, expireAfter);
    }
}
