package com.teammoeg.chorda.io.marshaller;

import java.util.ArrayList;
import java.util.List;

public class ListListWrapper<T> implements IList<T> {
	List<T> list;
	public ListListWrapper(List<T> list) {
		super();
		this.list = list;
	}
	public ListListWrapper(int size) {
		super();
		this.list = new ArrayList<>(size);
	}
	@Override
	public T get(int val) {
		return list.get(val);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public void add(Object object) {
		list.add((T) object);
	}

	@Override
	public Object getInstance() {
		return list;
	}

}
