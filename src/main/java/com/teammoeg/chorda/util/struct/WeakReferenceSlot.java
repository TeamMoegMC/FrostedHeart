package com.teammoeg.chorda.util.struct;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class WeakReferenceSlot<T> {
    private final WeakReference<T> resolved;
    private boolean isValid = true;
    private static final @NotNull WeakReferenceSlot<Void> EMPTY = new WeakReferenceSlot<>(null);

    public static <T> WeakReferenceSlot<T> of(final T instance) {
        return instance == null ? empty() : new WeakReferenceSlot<>(instance);
    }

    /**
     * @return The singleton empty instance
     */
    public static <T> WeakReferenceSlot<T> empty() {
        return EMPTY.cast();
    }

    @SuppressWarnings("unchecked")
    public <X> WeakReferenceSlot<X> cast() {
        return (WeakReferenceSlot<X>)this;
    }

    private WeakReferenceSlot(@Nullable T instance) {
    	if(instance!=null)
    		this.resolved = new WeakReference<>(instance);
    	else
    		this.resolved = null;
    }

    private @Nullable T getValue() {
        if (!isValid||resolved == null)
            return null;
        return resolved.get();
    }

    private T getValueUnsafe() {
        T ret = getValue();
        if (ret == null)
            throw new IllegalStateException("WeakReferenceSlot is empty or cleared");
        return ret;
    }
    public T getOrThrow() {
    	return getValueUnsafe();
    }
    
    public boolean isPresent() {
        return resolved != null && isValid && resolved.get()!=null;
    }

    public void ifPresent(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        T val = getValue();
        if (isValid && val != null)
            consumer.accept(val);
    }

    public <U> WeakReferenceSlot<U> valueMap(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return isPresent() ? of(mapper.apply(getValueUnsafe())) : empty();
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return isPresent() ? Optional.of(mapper.apply(getValueUnsafe())) : Optional.empty();
    }

    public Optional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        final T value = getValue(); // To keep the non-null contract we have to evaluate right now. Should we allow this function at all?
        return value != null && predicate.test(value) ? Optional.of(value) : Optional.empty();
    }

    public Optional<T> resolve() {
        return isPresent() ? Optional.of(getValueUnsafe()) : Optional.empty();
    }
    
    public T orElse(T other) {
        T val = getValue();
        return val != null ? val : other;
    }

    public T orElseGet(Supplier<? extends T> other) {
        T val = getValue();
        return val != null ? val : other.get();
    }
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        T val = getValue();
        if (val != null)
            return val;
        throw exceptionSupplier.get();
    }
    public void invalidate() {
        if (this.isValid) {
            this.isValid = false;
        }
    }
}
