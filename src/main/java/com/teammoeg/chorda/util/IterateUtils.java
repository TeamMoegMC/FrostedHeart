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

package com.teammoeg.chorda.util;

import java.util.Iterator;
import java.util.List;

import com.teammoeg.chorda.util.struct.MutablePair;

/**
 * 迭代工具类，提供将两个可迭代对象并行联合遍历的功能。
 * 支持AND模式（两者都有下一个元素时继续）和OR模式（任一有下一个元素时继续）。
 * <p>
 * Iteration utility class providing parallel joint traversal of two iterables.
 * Supports AND mode (continues when both have next) and OR mode (continues when either has next).
 */
public class IterateUtils {
	private static abstract class BiIterator<K,V> implements Iterator<MutablePair<K,V>>{
		protected Iterator<K> first;
		protected Iterator<V> second;
		MutablePair<K,V> pair=new MutablePair<>();
		public BiIterator(Iterator<K> first, Iterator<V> second) {
			super();
			this.first = first;
			this.second = second;
		}
	}
	private static class BiAndIterator<K,V> extends BiIterator<K,V>{
		public BiAndIterator(Iterator<K> first, Iterator<V> second) {
			super(first,second);
		}

		@Override
		public boolean hasNext() {
			return first.hasNext()&&second.hasNext();
		}

		@Override
		public MutablePair<K, V> next() {
			pair.set(first.next(), second.next());
			return pair;
		}
	}
	private static class BiOrIterator<K,V> extends BiIterator<K,V>{
		public BiOrIterator(Iterator<K> first, Iterator<V> second) {
			super(first,second);
		}

		@Override
		public boolean hasNext() {
			return first.hasNext()||second.hasNext();
		}

		@Override
		public MutablePair<K, V> next() {
			pair.set(first.hasNext()?first.next():null, second.hasNext()?second.next():null);
			return pair;
		}
	}
	/**
	 * 以AND模式联合两个可迭代对象，当两者都有下一个元素时产生配对。
	 * <p>
	 * Join two iterables in AND mode, producing pairs when both have a next element.
	 *
	 * @param <K> 第一个元素类型 / the first element type
	 * @param <V> 第二个元素类型 / the second element type
	 * @param first 第一个可迭代对象 / the first iterable
	 * @param second 第二个可迭代对象 / the second iterable
	 * @return 配对的可迭代对象 / the paired iterable
	 */
	public static <K,V> Iterable<MutablePair<K,V>> joinAnd(Iterable<K> first,Iterable<V> second){
		return new Iterable<MutablePair<K,V>>() {
			@Override
			public Iterator<MutablePair<K, V>> iterator() {
				return new BiAndIterator<>(first.iterator(),second.iterator());
			}
		};
	}
	/**
	 * 以OR模式联合两个可迭代对象，当任一有下一个元素时产生配对（缺失的一方为null）。
	 * <p>
	 * Join two iterables in OR mode, producing pairs when either has a next element
	 * (the missing side is null).
	 *
	 * @param <K> 第一个元素类型 / the first element type
	 * @param <V> 第二个元素类型 / the second element type
	 * @param first 第一个可迭代对象 / the first iterable
	 * @param second 第二个可迭代对象 / the second iterable
	 * @return 配对的可迭代对象 / the paired iterable
	 */
	public static <K,V> Iterable<MutablePair<K,V>> joinOr(Iterable<K> first,Iterable<V> second){
		return new Iterable<MutablePair<K,V>>() {
			@Override
			public Iterator<MutablePair<K, V>> iterator() {
				return new BiOrIterator<>(first.iterator(),second.iterator());
			}
		};
	}
	public static final Iterable<Boolean> boolIterable=List.of(true,false);
		
}
