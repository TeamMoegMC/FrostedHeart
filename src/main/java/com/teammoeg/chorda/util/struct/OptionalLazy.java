/*
 * Copyright (c) 2026 TeamMoeg
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
 * Forge的LazyOptional修改版，兼容null返回值。
 * <p>
 * A modified version of Forge's LazyOptional, compatible with null return values.
 *
 * @param <T> 包含值的类型 / the type of value contained
 * @deprecated 请使用 {@link net.minecraftforge.common.util.Lazy Lazy} 代替 / use {@link net.minecraftforge.common.util.Lazy Lazy} instead
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
     * 获取单例空实例。
     * <p>
     * Get the singleton empty instance.
     *
     * @param <T> 值类型 / the value type
     * @return 空的OptionalLazy实例 / the singleton empty instance
     */
    public static <T> OptionalLazy<T> empty() {
        return EMPTY.cast();
    }

    /**
     * 构造包装给定Supplier的新OptionalLazy。
     * <p>
     * Construct a new {@link OptionalLazy} that wraps the given Supplier.
     *
     * @param <T> 值类型 / the value type
     * @param instanceSupplier 要包装的Supplier，可以为null（返回空实例） / the Supplier to wrap, can be null (returns empty)
     * @return 新的OptionalLazy实例 / a new OptionalLazy instance
     */
    public static <T> OptionalLazy<T> of(final @Nullable Supplier<T> instanceSupplier) {
        return instanceSupplier == null ? empty() : new OptionalLazy<>(instanceSupplier);
    }

    /**
     * 从Optional的Supplier构造OptionalLazy。
     * <p>
     * Construct an OptionalLazy from a Supplier of Optional.
     *
     * @param <T> 值类型 / the value type
     * @param instanceSupplier Optional的Supplier / the Supplier of Optional
     * @return 新的OptionalLazy实例 / a new OptionalLazy instance
     */
    public static <T> OptionalLazy<T> ofOptional(final @Nullable Supplier<Optional<T>> instanceSupplier) {
        return instanceSupplier == null ? empty() : new OptionalLazy<>(()->instanceSupplier.get().orElse(null));
    }

    /**
     * 私有构造器。
     * <p>
     * Private constructor.
     *
     * @param instanceSupplier 值供应器 / the value supplier
     */
    private OptionalLazy(@Nullable Supplier<T> instanceSupplier) {
        this.supplier = instanceSupplier;
    }

    /**
     * 将此OptionalLazy强制转换为推断的泛型类型。仅在确定类型匹配时使用。
     * <p>
     * Cast this OptionalLazy to the inferred generic type. Only use when you are sure the type matches.
     *
     * @param <X> 目标类型 / the target type
     * @return 转换后的OptionalLazy / this OptionalLazy cast to the inferred type
     */
    @SuppressWarnings("unchecked")
    public <X> OptionalLazy<X> cast() {
        return (OptionalLazy<X>) this;
    }

    /**
     * 如果非空，解析供应器并用给定的谓词过滤结果。此方法不是懒加载的。
     * <p>
     * Resolve the contained supplier if non-empty, and filter it by the given
     * {@link NonNullPredicate}, returning empty if false. This method is not lazy.
     *
     * @param predicate 要应用于结果的谓词 / a predicate to apply to the result
     * @return 包含结果的Optional，如果谓词返回false则为空 / an Optional containing the result, or empty if predicate returns false
     */
    public Optional<T> filter(NonNullPredicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        final T value = getValue(); // To keep the non-null contract we have to evaluate right now. Should we allow this function at all?
        return value != null && predicate.test(value) ? Optional.of(value) : Optional.empty();
    }

    /**
     * 获取内部值，使用双重检查锁定确保线程安全的懒加载。
     * <p>
     * Get the internal value, using double-checked locking for thread-safe lazy initialization.
     *
     * @return 解析后的值，如果无效或供应器为null则返回null / the resolved value, or null if invalid or supplier is null
     */
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
     * 如果非空，使用包含的对象调用指定的消费者。
     * <p>
     * If non-empty, invoke the specified consumer with the contained object.
     *
     * @param consumer 非空时执行的消费者 / the consumer to run if non-empty
     */
    public void ifPresent(NonNullConsumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        T val = getValue();
        if (isValid && val != null)
            consumer.accept(val);
    }

    /**
     * 检查此OptionalLazy是否非空。
     * <p>
     * Check if this OptionalLazy is non-empty.
     *
     * @return 如果持有非null的供应器返回true / true if this holds a non-null supplier
     */
    public boolean isPresent() {
        if (supplier == null || !isValid) return false;
        return getValue() != null;
    }

    /**
     * 如果非空，返回封装映射函数的新OptionalLazy。内部供应器不会被解析。
     * <p>
     * If non-empty, return a new OptionalLazy encapsulating the mapping function.
     * The supplier inside is NOT resolved.
     *
     * @param <U> 映射结果类型 / the mapped result type
     * @param mapper 映射函数 / the mapping function
     * @return 映射后的OptionalLazy / the mapped OptionalLazy
     */
    public <U> OptionalLazy<U> lazyMap(NonNullFunction<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return isPresent() ? of(() -> mapper.apply(getValue())) : empty();
    }

    /**
     * 如果非空，返回封装映射值的新Optional。此方法会解析供应器的值。
     * <p>
     * If non-empty, return a new Optional encapsulating the mapped value.
     * This method explicitly resolves the supplier value.
     *
     * @param <U> 映射结果类型 / the mapped result type
     * @param mapper 映射函数 / the mapping function
     * @return 映射后的Optional / the mapped Optional
     */
    public <U> Optional<U> map(NonNullFunction<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return isPresent() ? Optional.of(mapper.apply(getValue())) : Optional.empty();
    }

    /**
     * 解析供应器并返回结果，如果为空则返回备选值。
     * <p>
     * Resolve the supplier and return the result, or return the alternative value if empty.
     *
     * @param other 为空时返回的备选值 / the alternative value to return if empty
     * @return 供应器结果或备选值 / the supplier result or the alternative value
     */
    public T orElse(T other) {
        T val = getValue();
        return val != null ? val : other;
    }

    /**
     * 解析供应器并返回结果，如果为空则返回备选供应器的结果。
     * <p>
     * Resolve the supplier and return the result, or return the result of the alternative supplier if empty.
     *
     * @param other 为空时使用的备选供应器 / the alternative supplier to use if empty
     * @return 供应器结果或备选供应器结果 / the supplier result or the alternative supplier result
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
    /**
     * 解析并获取值，如果为空则抛出NoSuchElementException。
     * <p>
     * Resolve and get the value, throwing NoSuchElementException if empty.
     *
     * @return 解析后的值 / the resolved value
     * @throws NoSuchElementException 如果对象不存在 / if the object is not present
     */
    public T get() {
        T val = getValue();
        if (val != null)
            return val;
        throw new NoSuchElementException("Object not present");
    }

	/**
	 * 检查值是否已被解析。
	 * <p>
	 * Check if the value has been resolved.
	 *
	 * @return 如果已解析返回true / true if resolved
	 */
	public boolean isResolved() {
		return isResolved;
	}
}