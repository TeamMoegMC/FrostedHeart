/*
 * Copyright (c) 2026 TeamMoeg
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
