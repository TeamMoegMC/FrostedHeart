/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;


public class CFunctionUtils {

	public CFunctionUtils() {
		
	}
	public static <A,B> Function<A,B> mapNullable(Function<A,B> func,B def){
		return a->a==null?def:func.apply(a);
	}
	public static <A,B,C> Function<A,C> mapIfMapNullable(Function<A,B> func,Function<B,C> def,Function<A,C> ifNull){
		return a->{
			B b=func.apply(a);
			if(b!=null) {
				return def.apply(b);
			}
			return ifNull.apply(a);
		};
	}
	public static <T> T makeIf(boolean available,Supplier<T> supp) {
		if(available)
			return supp.get();
		return null;
	}
	public static <T> List<T> generate(int startInclusive,int endExclusive,IntFunction<T> generator) {
		List<T> list=new ArrayList<>();
		for(int i=startInclusive;i<endExclusive;i++)
			list.add(generator.apply(i));
		return list;
	}
	public static <O,T> List<T> generate(O[] types,Function<O,T> generator) {
		List<T> list=new ArrayList<>();
		for(O obj:types)
			list.add(generator.apply(obj));
		return list;
	}
	public static <O,T> List<T> generate(Collection<O> types,Function<O,T> generator) {
		List<T> list=new ArrayList<>();
		for(O obj:types)
			list.add(generator.apply(obj));
		return list;
	}
}
