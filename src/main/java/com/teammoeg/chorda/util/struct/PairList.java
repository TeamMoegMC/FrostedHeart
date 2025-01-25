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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;

public class PairList<K,V> extends ArrayList<Pair<K,V>> {
	private static final long serialVersionUID = 29730037267288743L;
	public PairList() {
		super();
	}
	public PairList(Map<K, V> c) {
		super(toCollection(c));
	}
	public PairList(int initialCapacity) {
		super(initialCapacity);
	}
	public boolean add(K e1,V e2) {
		return super.add(Pair.of(e1, e2));
	}
	public void add(int index, K e1,V e2) {
		super.add(index, Pair.of(e1, e2));
	}
	public boolean remove(K e1,V e2) {
		return super.remove(Pair.of(e1, e2));
	}
	public boolean addAll(Map<K, V> c) {
		return super.addAll(toCollection(c));
	}
	private static <K,V> Collection<Pair<K,V>> toCollection(Map<K,V> map) {
		return map.entrySet().stream().map(t->Pair.of(t.getKey(), t.getValue())).collect(Collectors.toList());
	}
	public boolean removeAll(Map<K, V> c) {
		return super.removeAll(toCollection(c));
	}
	public void forEach(BiConsumer<K, V> action) {
		super.forEach(t->action.accept(t.getFirst(), t.getSecond()));
	}
	public void replaceAll(BiFunction<K, V, Pair<K,V>> action) {
		super.replaceAll(t->action.apply(t.getFirst(), t.getSecond()));
	}
	public boolean removeIf(BiPredicate<K,V> filter) {
		return super.removeIf(t->filter.test(t.getFirst(), t.getSecond()));
	}
	public boolean containsAll(Map<K, V> c) {
		return super.containsAll(toCollection(c));
	}
	public PairList(Collection<? extends Pair<K, V>> c) {
		super(c);
	}
}
