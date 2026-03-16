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
 * 结果缓存迭代器，将单个生成函数包装为Iterator接口。
 * 子类实现{@link #internalNext()}提供下一个元素，返回null表示结束。
 * <p>
 * Result-caching iterator that wraps a single generator function into an Iterator interface.
 * Subclasses implement {@link #internalNext()} to provide the next element, returning null to indicate end.
 *
 * @param <E> 元素类型 / the element type
 */
public abstract class ResultCacheIterator<E> implements Iterator<E> {
	E cached;
	boolean hasCached;
	boolean isEnded;
	public ResultCacheIterator() {
	}
	/**
	 * 内部获取下一个元素。返回null表示迭代结束。
	 * <p>
	 * Internally get the next element. Return null to indicate end of iteration.
	 *
	 * @return 下一个元素，或null表示结束 / the next element, or null to indicate end
	 */
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
