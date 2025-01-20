package com.teammoeg.chorda.util.io;

import java.util.AbstractList;
import java.util.Map;

public abstract class RegistryListedMap<K,E> extends AbstractList<E> {
	Map<K,E> map;
	int msize;
	public RegistryListedMap(Map<K,E> map,int maxsize) {
		this.map=map;
		msize=maxsize;
	}

	public abstract K getKey(int id);
	@Override
	public E get(int index) {
		return map.get(getKey(index));
	}
	@Override
	public int size() {
		return msize;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsValue(o);
	}

	@Override
	public boolean remove(Object o) {
		return map.values().remove(o);
	}
	@Override
	public void clear() {
		map.clear();
	}
	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

}
