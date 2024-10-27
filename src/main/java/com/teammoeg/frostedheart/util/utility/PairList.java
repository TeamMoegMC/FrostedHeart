package com.teammoeg.frostedheart.util.utility;

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
