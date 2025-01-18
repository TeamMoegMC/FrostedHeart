package com.teammoeg.chorda.util.misc;

import java.util.Iterator;
import java.util.NoSuchElementException;
/** 
 * Wraper to wrap a single function to an iterator
 * */
public abstract class ResultCacheIterator<E> implements Iterator<E> {
	E cached;
	boolean hasCached;
	boolean isEnded;
	public ResultCacheIterator() {
	}
	protected abstract E internalNext() ;
	
	@Override
	public boolean hasNext() {
		if(!hasCached) {
			cached=internalNext();
			if(cached!=null)
				hasCached=true;
			else 
				isEnded=true;
		}
		return cached!=null;
	}

	@Override
	public E next() {
		if(!isEnded&&!hasCached) {
			hasNext();
		}
		if(isEnded) {
			throw new NoSuchElementException();
		}
		hasCached=false;
		E result=cached;
		cached=null;
		return result;
	}

}
