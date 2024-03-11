package com.teammoeg.frostedheart.util.utility;

import java.util.Map;
import java.util.Objects;

import com.mojang.datafixers.util.Pair;

public class MutablePair<K,V> {
	private K first;
	private V second;
	public MutablePair(K first,V second) {
		set(first,second);
	}
	public MutablePair(Pair<K,V> pair) {
		set(pair.getFirst(), pair.getSecond());
	}
	public MutablePair(Map.Entry<K,V> pair) {
		set(pair.getKey(), pair.getValue());
	}
	public MutablePair() {
		super();
	}
	public void set(K first,V second) {
		this.first=first;
		this.second=second;
	}
	public K getFirst() {
		return first;
	}
	public void setFirst(K first) {
		this.first = first;
	}
	public V getSecond() {
		return second;
	}
	public void setSecond(V second) {
		this.second = second;
	}
	public static <K,V> MutablePair<K,V> of(K first,V second) {
		return new MutablePair<K,V>(first,second);
	}
	public static <K,V> MutablePair<K,V> of(Pair<K,V> pair) {
		return new MutablePair<K,V>(pair.getFirst(), pair.getSecond());
	}
	public static <K,V> MutablePair<K,V> of(Map.Entry<K,V> pair) {
		return new MutablePair<K,V>(pair.getKey(), pair.getValue());
	}
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
