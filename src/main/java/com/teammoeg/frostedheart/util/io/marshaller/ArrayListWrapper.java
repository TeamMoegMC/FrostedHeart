package com.teammoeg.frostedheart.util.io.marshaller;

public class ArrayListWrapper<T> implements IList<T> {
	T[] array;
	int cridx=0;
	public ArrayListWrapper(T[] array) {
		super();
		this.array = array;
	}
	public ArrayListWrapper(Object array) {
		super();
		this.array = (T[]) array;
	}

	@Override
	public T get(int val) {
		return array[val];
	}

	@Override
	public int size() {
		return array.length;
	}

	@Override
	public void add(Object object) {
		array[cridx++]=(T) object;
	}

	@Override
	public Object getInstance() {
		return array;
	}

}
