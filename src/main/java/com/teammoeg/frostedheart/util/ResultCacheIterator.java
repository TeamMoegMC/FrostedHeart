package com.teammoeg.frostedheart.util;

import java.util.Iterator;

public abstract class ResultCacheIterator<E> implements Iterator<E> {
	E cached;
	boolean hasCached;
	public ResultCacheIterator() {
	}
	protected abstract E internalNext() ;
	
	@Override
	public boolean hasNext() {
		if(!hasCached) {
			cached=internalNext();
			hasCached=true;
		}
		return cached!=null;
	}

	@Override
	public E next() {
		hasCached=false;
		E result=cached;
		cached=null;
		return result;
	}

}
