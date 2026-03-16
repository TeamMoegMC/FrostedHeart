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

import java.util.Map;
import java.util.Objects;

import com.mojang.datafixers.util.Pair;

/**
 * 可变键值对，允许在创建后修改两个元素的值。
 * 与不可变的Pair不同，此类支持就地修改以减少对象分配。
 * <p>
 * Mutable key-value pair allowing modification of both elements after creation.
 * Unlike immutable Pair, this class supports in-place modification to reduce object allocation.
 *
 * @param <K> 第一个元素类型 / the first element type
 * @param <V> 第二个元素类型 / the second element type
 */
public class MutablePair<K,V> {
	private K first;
	private V second;

	/**
	 * 使用两个元素构造可变键值对。
	 * <p>
	 * Construct a mutable pair with two elements.
	 *
	 * @param first 第一个元素 / the first element
	 * @param second 第二个元素 / the second element
	 */
	public MutablePair(K first,V second) {
		set(first,second);
	}

	/**
	 * 从Pair构造可变键值对。
	 * <p>
	 * Construct a mutable pair from a Pair.
	 *
	 * @param pair 源Pair / the source Pair
	 */
	public MutablePair(Pair<K,V> pair) {
		set(pair.getFirst(), pair.getSecond());
	}

	/**
	 * 从Map.Entry构造可变键值对。
	 * <p>
	 * Construct a mutable pair from a Map.Entry.
	 *
	 * @param pair 源Map.Entry / the source Map.Entry
	 */
	public MutablePair(Map.Entry<K,V> pair) {
		set(pair.getKey(), pair.getValue());
	}

	/**
	 * 构造空的可变键值对。
	 * <p>
	 * Construct an empty mutable pair.
	 */
	public MutablePair() {
		super();
	}
	/**
	 * 设置两个元素的值。
	 * <p>
	 * Set both element values.
	 *
	 * @param first 第一个元素 / the first element
	 * @param second 第二个元素 / the second element
	 */
	public void set(K first,V second) {
		this.first=first;
		this.second=second;
	}
	/**
	 * 获取第一个元素。
	 * <p>
	 * Get the first element.
	 *
	 * @return 第一个元素 / the first element
	 */
	public K getFirst() {
		return first;
	}

	/**
	 * 设置第一个元素。
	 * <p>
	 * Set the first element.
	 *
	 * @param first 第一个元素 / the first element
	 */
	public void setFirst(K first) {
		this.first = first;
	}

	/**
	 * 获取第二个元素。
	 * <p>
	 * Get the second element.
	 *
	 * @return 第二个元素 / the second element
	 */
	public V getSecond() {
		return second;
	}

	/**
	 * 设置第二个元素。
	 * <p>
	 * Set the second element.
	 *
	 * @param second 第二个元素 / the second element
	 */
	public void setSecond(V second) {
		this.second = second;
	}
	/**
	 * 创建包含两个元素的可变键值对。
	 * <p>
	 * Create a mutable pair with two elements.
	 *
	 * @param <K> 第一个元素类型 / the first element type
	 * @param <V> 第二个元素类型 / the second element type
	 * @param first 第一个元素 / the first element
	 * @param second 第二个元素 / the second element
	 * @return 新的可变键值对 / a new mutable pair
	 */
	public static <K,V> MutablePair<K,V> of(K first,V second) {
		return new MutablePair<K,V>(first,second);
	}
	/**
	 * 从Pair创建可变键值对。
	 * <p>
	 * Create a mutable pair from a Pair.
	 *
	 * @param <K> 第一个元素类型 / the first element type
	 * @param <V> 第二个元素类型 / the second element type
	 * @param pair 源Pair / the source Pair
	 * @return 新的可变键值对 / a new mutable pair
	 */
	public static <K,V> MutablePair<K,V> of(Pair<K,V> pair) {
		return new MutablePair<K,V>(pair.getFirst(), pair.getSecond());
	}

	/**
	 * 从Map.Entry创建可变键值对。
	 * <p>
	 * Create a mutable pair from a Map.Entry.
	 *
	 * @param <K> 第一个元素类型 / the first element type
	 * @param <V> 第二个元素类型 / the second element type
	 * @param pair 源Map.Entry / the source Map.Entry
	 * @return 新的可变键值对 / a new mutable pair
	 */
	public static <K,V> MutablePair<K,V> of(Map.Entry<K,V> pair) {
		return new MutablePair<K,V>(pair.getKey(), pair.getValue());
	}

	/**
	 * 创建空的可变键值对。
	 * <p>
	 * Create an empty mutable pair.
	 *
	 * @param <K> 第一个元素类型 / the first element type
	 * @param <V> 第二个元素类型 / the second element type
	 * @return 新的空可变键值对 / a new empty mutable pair
	 */
	public static <K,V> MutablePair<K,V> of() {
		return new MutablePair<K,V>();
	}
	@Override
	public int hashCode() {
		return Objects.hash(first, second);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		MutablePair other = (MutablePair) obj;
		return Objects.equals(first, other.first) && Objects.equals(second, other.second);
	}
}
