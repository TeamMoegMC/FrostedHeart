package com.teammoeg.chorda.io.marshaller;

public interface IList<T> {
	T get(int val);
	int size();
	void add(Object object);
	Object getInstance();
}
