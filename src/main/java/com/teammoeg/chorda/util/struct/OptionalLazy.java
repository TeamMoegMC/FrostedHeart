/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.util.struct;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.common.util.NonNullFunction;
import net.minecraftforge.common.util.NonNullPredicate;
import net.minecraftforge.common.util.NonNullSupplier;

/**
 * An modified version for LazyOptional by forge, compatibilities for null return
 * @deprecated use {@link net.minecraftforge.common.util.Lazy Lazy} instead
 */
@Deprecated
public class OptionalLazy<T> {
    private static final @Nonnull
    OptionalLazy<Void> EMPTY = new OptionalLazy<>(null);
    private final Supplier<T> supplier;
    private final Object lock = new Object();
    private T value;
    private boolean isResolved;
    private boolean isValid = true;

    /**
     * @return The singleton empty instance
     */
    public static <T> OptionalLazy<T> empty() {
        return EMPTY.cast();
    }

    /**
     * Construct a new {@link OptionalLazy} that wraps the given
     * {@link NonNullSupplier}.
     *
     * @param instanceSupplier The {@link NonNullSupplier} to wrap. Cannot return
     *                         null, but can be null itself. If null, this method
     *                         returns {@link #empty()}.
     */
    public static <T> OptionalLazy<T> of(final @Nullable Supplier<T> instanceSupplier) {
        return instanceSupplier == null ? empty() : new OptionalLazy<>(instanceSupplier);
    }
    public static <T> OptionalLazy<T> ofOptional(final @Nullable Supplier<Optional<T>> instanceSupplier) {
        return instanceSupplier == null ? empty() : new OptionalLazy<>(()->instanceSupplier.get().orElse(null));
    }
    private OptionalLazy(@Nullable Supplier<T> instanceSupplier) {
        this.supplier = instanceSupplier;
    }
    /**
     * This method hides an unchecked cast to the inferred type. Only use this if
     * you are sure the type should match. For capabilities, generally
     * {@link Capability#orEmpty(Capability, LazyOptional)} should be used.
     *
     * @return This {@link OptionalLazy}, cast to the inferred generic type
     */
    @SuppressWarnings("unchecked")
    public <X> OptionalLazy<X> cast() {
        return (OptionalLazy<X>) this;
    }

    /**
     * Resolve the contained supplier if non-empty, and filter it by the given
     * {@link NonNullPredicate}, returning empty if false.
     * <p>
     * <em>It is important to note that this method is <strong>not</strong> lazy, as
     * it must resolve the value of the supplier to validate it with the
     * predicate.</em>
     *
     * @param predicate A {@link NonNullPredicate} to apply to the result of the
     *                  contained supplier, if non-empty
     * @return An {@link Optional} containing the result of the contained
     * supplier, if and only if the passed {@link NonNullPredicate} returns
     * true, otherwise an empty {@link Optional}
     * @throws NullPointerException If {@code predicate} is null and this
     *                              {@link Optional} is non-empty
     */
    public Optional<T> filter(NonNullPredicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        final T value = getValue(); // To keep the non-null contract we have to evaluate right now. Should we allow this function at all?
        return value != null && predicate.test(value) ? Optional.of(value) : Optional.empty();
    }

    private @Nullable T getValue() {
        if (!isValid || supplier == null)
            return null;
        if (!isResolved) {
            synchronized (lock) {
                // resolved == null: Double checked locking to prevent two threads from resolving
                if (!isResolved) {
                    value = supplier.get();
                    isResolved=true;
                }
            }
        }
        return value;
    }

    /**
     * If non-empty, invoke the specified {@link NonNullConsumer} with the object,
     * otherwise do nothing.
     *
     * @param consumer The {@link NonNullConsumer} to run if this optional is non-empty.
     * @throws NullPointerException if {@code consumer} is null and this {@link OptionalLazy} is non-empty
     */
    public void ifPresent(NonNullConsumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        T val = getValue();
        if (isValid && val != null)
            consumer.accept(val);
    }

    /**
     * Check if this {@link OptionalLazy} is non-empty.
     *
     * @return {@code true} if this {@link OptionalLazy} is non-empty, i.e. holds a
     * non-null supplier
     */
    public boolean isPresent() {
        if (supplier == null || !isValid) return false;
        return getValue() != null;
    }

    /**
     * If a this {@link OptionalLazy} is non-empty, return a new
     * {@link OptionalLazy} encapsulating the mapping function. Otherwise, returns
     * {@link #empty()}.
     * <p>
     * The supplier inside this object is <strong>NOT</strong> resolved.
     *
     * @param mapper A mapping function to apply to the mod object, if present
     * @return A {@link OptionalLazy} describing the result of applying a mapping
     * function to the value of this {@link OptionalLazy}, if a value is
     * present, otherwise an empty {@link OptionalLazy}
     * @throws NullPointerException if {@code mapper} is null.
     * @apiNote This method supports post-processing on optional values, without the
     * need to explicitly check for a return status.
     * @apiNote The returned value does not receive invalidation messages from the original {@link OptionalLazy}.
     * If you need the invalidation, you will need to manage them yourself.
     */
    public <U> OptionalLazy<U> lazyMap(NonNullFunction<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return isPresent() ? of(() -> mapper.apply(getValue())) : empty();
    }

    /**
     * If a this {@link OptionalLazy} is non-empty, return a new
     * {@link Optional} encapsulating the mapped value. Otherwise, returns
     * {@link Optional#empty()}.
     *
     * @param mapper A mapping function to apply to the mod object, if present
     * @return An {@link Optional} describing the result of applying a mapping
     * function to the value of this {@link Optional}, if a value is
     * present, otherwise an empty {@link Optional}
     * @throws NullPointerException if {@code mapper} is null.
     * @apiNote This method explicitly resolves the value of the {@link OptionalLazy}.
     * For a non-resolving mapper that will lazily run the mapping, use {@link #lazyMap(NonNullFunction)}.
     */
    public <U> Optional<U> map(NonNullFunction<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return isPresent() ? Optional.of(mapper.apply(getValue())) : Optional.empty();
    }

    /**
     * Resolve the contained supplier if non-empty and return the result, otherwise return
     * {@code other}.
     *
     * @param other the value to be returned if this {@link OptionalLazy} is empty
     * @return the result of the supplier, if non-empty, otherwise {@code other}
     */
    public T orElse(T other) {
        T val = getValue();
        return val != null ? val : other;
    }

    /**
     * Resolve the contained supplier if non-empty and return the result, otherwise return the
     * result of {@code other}.
     *
     * @param other A {@link NonNullSupplier} whose result is returned if this
     *              {@link OptionalLazy} is empty
     * @return The result of the supplier, if non-empty, otherwise the result of
     * {@code other.get()}
     * @throws NullPointerException If {@code other} is null and this
     *                              {@link OptionalLazy} is non-empty
     */
    public T orElseGet(Supplier<? extends T> other) {
        T val = getValue();
        return val != null ? val : other.get();
    }

    /**
     * Resolve the contained supplier if non-empty and return the result, otherwise throw the
     * exception created by the provided {@link NonNullSupplier}.
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionSupplier The {@link NonNullSupplier} which will return the
     *                          exception to be thrown
     * @return The result of the supplier
     * @throws X                    If this {@link OptionalLazy} is empty
     * @throws NullPointerException If {@code exceptionSupplier} is null and this
     *                              {@link OptionalLazy} is empty
     * @apiNote A method reference to the exception constructor with an empty
     * argument list can be used as the supplier. For example,
     * {@code IllegalStateException::new}
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        T val = getValue();
        if (val != null)
            return val;
        throw exceptionSupplier.get();
    }

    /**
     * Resolves the value of this LazyOptional, turning it into a standard non-lazy {@link Optional<T>}
     *
     * @return The resolved optional.
     */
    public Optional<T> resolve() {
        return isPresent() ? Optional.ofNullable(getValue()) : Optional.empty();
    }
    public T get() {
        T val = getValue();
        if (val != null)
            return val;
        throw new NoSuchElementException("Object not present");
    }

	public boolean isResolved() {
		return isResolved;
	}
}