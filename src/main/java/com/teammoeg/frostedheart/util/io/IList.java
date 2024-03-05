package com.teammoeg.frostedheart.util.io;

public interface IList<T> {
	T get(int val);
	int size();
	void add(Object object);
	Object getInstance();
}
