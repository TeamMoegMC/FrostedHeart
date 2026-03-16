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

package com.teammoeg.chorda.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;


/**
 * 函数式编程工具类，提供函数组合、空值安全映射和列表生成等辅助方法。
 * <p>
 * Functional programming utility class providing function composition,
 * null-safe mapping and list generation helper methods.
 */
public class CFunctionUtils {

	public CFunctionUtils() {

	}
	/**
	 * 创建空值安全的映射函数，输入为null时返回默认值。
	 * <p>
	 * Create a null-safe mapping function that returns a default value when input is null.
	 *
	 * @param <A> 输入类型 / the input type
	 * @param <B> 输出类型 / the output type
	 * @param func 映射函数 / the mapping function
	 * @param def 默认值 / the default value
	 * @return 空值安全的函数 / a null-safe function
	 */
	public static <A,B> Function<A,B> mapNullable(Function<A,B> func,B def){
		return a->a==null?def:func.apply(a);
	}
	/**
	 * 创建条件映射函数：先用func映射，如果中间结果非null则用def映射，否则用ifNull映射。
	 * <p>
	 * Create a conditional mapping function: first map with func, if the intermediate result
	 * is non-null then map with def, otherwise map with ifNull.
	 *
	 * @param <A> 输入类型 / the input type
	 * @param <B> 中间类型 / the intermediate type
	 * @param <C> 输出类型 / the output type
	 * @param func 第一步映射函数 / the first mapping function
	 * @param def 非null时的映射函数 / the mapping function when non-null
	 * @param ifNull null时的回退函数 / the fallback function when null
	 * @return 组合函数 / the composed function
	 */
	public static <A,B,C> Function<A,C> mapIfMapNullable(Function<A,B> func,Function<B,C> def,Function<A,C> ifNull){
		return a->{
			B b=func.apply(a);
			if(b!=null) {
				return def.apply(b);
			}
			return ifNull.apply(a);
		};
	}
	/**
	 * 条件创建对象：条件为true时通过Supplier创建对象，否则返回null。
	 * <p>
	 * Conditionally create an object: create via Supplier when condition is true, otherwise return null.
	 *
	 * @param <T> 对象类型 / the object type
	 * @param available 条件 / the condition
	 * @param supp 对象供应器 / the object supplier
	 * @return 创建的对象或null / the created object or null
	 */
	public static <T> T makeIf(boolean available,Supplier<T> supp) {
		if(available)
			return supp.get();
		return null;
	}
	/**
	 * 通过索引范围和生成函数生成列表。
	 * <p>
	 * Generate a list from an index range and a generator function.
	 *
	 * @param <T> 元素类型 / the element type
	 * @param startInclusive 起始索引（包含） / the start index (inclusive)
	 * @param endExclusive 结束索引（不包含） / the end index (exclusive)
	 * @param generator 生成函数 / the generator function
	 * @return 生成的列表 / the generated list
	 */
	public static <T> List<T> generate(int startInclusive,int endExclusive,IntFunction<T> generator) {
		List<T> list=new ArrayList<>();
		for(int i=startInclusive;i<endExclusive;i++)
			list.add(generator.apply(i));
		return list;
	}
	/**
	 * 通过数组和映射函数生成列表。
	 * <p>
	 * Generate a list from an array and a mapping function.
	 *
	 * @param <O> 源元素类型 / the source element type
	 * @param <T> 目标元素类型 / the target element type
	 * @param types 源数组 / the source array
	 * @param generator 映射函数 / the mapping function
	 * @return 生成的列表 / the generated list
	 */
	public static <O,T> List<T> generate(O[] types,Function<O,T> generator) {
		List<T> list=new ArrayList<>();
		for(O obj:types)
			list.add(generator.apply(obj));
		return list;
	}
	/**
	 * 通过集合和映射函数生成列表。
	 * <p>
	 * Generate a list from a collection and a mapping function.
	 *
	 * @param <O> 源元素类型 / the source element type
	 * @param <T> 目标元素类型 / the target element type
	 * @param types 源集合 / the source collection
	 * @param generator 映射函数 / the mapping function
	 * @return 生成的列表 / the generated list
	 */
	public static <O,T> List<T> generate(Collection<O> types,Function<O,T> generator) {
		List<T> list=new ArrayList<>();
		for(O obj:types)
			list.add(generator.apply(obj));
		return list;
	}
}
