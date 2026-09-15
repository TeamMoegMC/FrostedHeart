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

import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

/**
 * 柯里化应用函子模板，提供从1到32个参数的函数接口和类型安全的构建器模式。
 * 用于构建具有多个参数的解析器或编解码器组合。
 * <p>
 * Curried applicative functor template providing function interfaces from 1 to 32 parameters
 * and a type-safe builder pattern. Used for constructing parsers or codec combinations
 * with multiple parameters.
 */
public class CurryApplicativeTemplate {
	/**
	 * 私有构造器，防止实例化此工具类。
	 * <p>
	 * Private constructor to prevent instantiation of this utility class.
	 */
	private CurryApplicativeTemplate() {

	}

	/**
	 * 接受1个参数的函数接口。
	 * <p>
	 * Function interface accepting 1 parameter.
	 *
	 * @param <O> 返回类型 / the return type
	 * @param <A> 参数类型 / the parameter type
	 */
	public static interface Function1<O, A> {
		O apply(A a);

		/**
		 * 转换为 java.util.function.Function。
		 * <p>
		 * Convert to a java.util.function.Function.
		 *
		 * @return 等价的 Function / the equivalent Function
		 */
		default Function<A, O> toFunction() {
			return this::apply;
		}
	}

	public static interface Function2<O, A, B> {
		O apply(A a, B b);

		/**
		 * 转换为 java.util.function.BiFunction。
		 * <p>
		 * Convert to a java.util.function.BiFunction.
		 *
		 * @return 等价的 BiFunction / the equivalent BiFunction
		 */
		default BiFunction<A, B, O> toBiFunction() {
			return this::apply;
		}

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function1<Function1<O, B>, A> curry1() {
			return a -> b -> apply(a, b);
		}
	}

	public static interface Function3<O, A, B, C> {
		O apply(A a, B b, C c);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 2 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 2.
		 */
		default Function1<Function2<O, B, C>, A> curry1() {
			return a -> (b, c) -> apply(a, b, c);
		}

		/**
		 * 返回柯里化函数：接受前 2 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 2 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function2<Function1<O, C>, A, B> curry2() {
			return (a, b) -> c -> apply(a, b, c);
		}
	}

	public static interface Function4<O, A, B, C, D> {
		O apply(A a, B b, C c, D d);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 3 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 3.
		 */
		default Function1<Function3<O, B, C, D>, A> curry1() {
			return a -> (b, c, d) -> apply(a, b, c, d);
		}

		/**
		 * 返回柯里化函数：接受前 2 个参数，再返回接受剩余 2 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 2 parameter(s) and returning a function taking the remaining 2.
		 */
		default Function2<Function2<O, C, D>, A, B> curry2() {
			return (a, b) -> (c, d) -> apply(a, b, c, d);
		}

		/**
		 * 返回柯里化函数：接受前 3 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 3 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function3<Function1<O, D>, A, B, C> curry3() {
			return (a, b, c) -> d -> apply(a, b, c, d);
		}
	}

	public static interface Function5<O, A, B, C, D, E> {
		O apply(A a, B b, C c, D d, E e);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 4 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 4.
		 */
		default Function1<Function4<O, B, C, D, E>, A> curry1() {
			return a -> (b, c, d, e) -> apply(a, b, c, d, e);
		}

		/**
		 * 返回柯里化函数：接受前 2 个参数，再返回接受剩余 3 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 2 parameter(s) and returning a function taking the remaining 3.
		 */
		default Function2<Function3<O, C, D, E>, A, B> curry2() {
			return (a, b) -> (c, d, e) -> apply(a, b, c, d, e);
		}

		/**
		 * 返回柯里化函数：接受前 4 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 4 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function4<Function1<O, E>, A, B, C, D> curry4() {
			return (a, b, c, d) -> e -> apply(a, b, c, d, e);
		}
	}

	public static interface Function6<O, A, B, C, D, E, F> {
		O apply(A a, B b, C c, D d, E e, F f);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 5 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 5.
		 */
		default Function1<Function5<O, B, C, D, E, F>, A> curry1() {
			return a -> (b, c, d, e, f) -> apply(a, b, c, d, e, f);
		}

		/**
		 * 返回柯里化函数：接受前 2 个参数，再返回接受剩余 4 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 2 parameter(s) and returning a function taking the remaining 4.
		 */
		default Function2<Function4<O, C, D, E, F>, A, B> curry2() {
			return (a, b) -> (c, d, e, f) -> apply(a, b, c, d, e, f);
		}

		/**
		 * 返回柯里化函数：接受前 3 个参数，再返回接受剩余 3 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 3 parameter(s) and returning a function taking the remaining 3.
		 */
		default Function3<Function3<O, D, E, F>, A, B, C> curry3() {
			return (a, b, c) -> (d, e, f) -> apply(a, b, c, d, e, f);
		}

		/**
		 * 返回柯里化函数：接受前 5 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 5 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function5<Function1<O, F>, A, B, C, D, E> curry5() {
			return (a, b, c, d, e) -> f -> apply(a, b, c, d, e, f);
		}
	}

	public static interface Function7<O, A, B, C, D, E, F, G> {
		O apply(A a, B b, C c, D d, E e, F f, G g);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 6 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 6.
		 */
		default Function1<Function6<O, B, C, D, E, F, G>, A> curry1() {
			return a -> (b, c, d, e, f, g) -> apply(a, b, c, d, e, f, g);
		}

		/**
		 * 返回柯里化函数：接受前 2 个参数，再返回接受剩余 5 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 2 parameter(s) and returning a function taking the remaining 5.
		 */
		default Function2<Function5<O, C, D, E, F, G>, A, B> curry2() {
			return (a, b) -> (c, d, e, f, g) -> apply(a, b, c, d, e, f, g);
		}

		/**
		 * 返回柯里化函数：接受前 3 个参数，再返回接受剩余 4 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 3 parameter(s) and returning a function taking the remaining 4.
		 */
		default Function3<Function4<O, D, E, F, G>, A, B, C> curry3() {
			return (a, b, c) -> (d, e, f, g) -> apply(a, b, c, d, e, f, g);
		}

		/**
		 * 返回柯里化函数：接受前 6 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 6 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function6<Function1<O, G>, A, B, C, D, E, F> curry6() {
			return (a, b, c, d, e, f) -> g -> apply(a, b, c, d, e, f, g);
		}
	}

	public static interface Function8<O, A, B, C, D, E, F, G, H> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 7 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 7.
		 */
		default Function1<Function7<O, B, C, D, E, F, G, H>, A> curry1() {
			return a -> (b, c, d, e, f, g, h) -> apply(a, b, c, d, e, f, g, h);
		}

		/**
		 * 返回柯里化函数：接受前 3 个参数，再返回接受剩余 5 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 3 parameter(s) and returning a function taking the remaining 5.
		 */
		default Function3<Function5<O, D, E, F, G, H>, A, B, C> curry3() {
			return (a, b, c) -> (d, e, f, g, h) -> apply(a, b, c, d, e, f, g, h);
		}

		/**
		 * 返回柯里化函数：接受前 4 个参数，再返回接受剩余 4 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 4 parameter(s) and returning a function taking the remaining 4.
		 */
		default Function4<Function4<O, E, F, G, H>, A, B, C, D> curry4() {
			return (a, b, c, d) -> (e, f, g, h) -> apply(a, b, c, d, e, f, g, h);
		}

		/**
		 * 返回柯里化函数：接受前 7 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 7 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function7<Function1<O, H>, A, B, C, D, E, F, G> curry7() {
			return (a, b, c, d, e, f, g) -> h -> apply(a, b, c, d, e, f, g, h);
		}
	}

	public static interface Function9<O, A, B, C, D, E, F, G, H, I> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 8 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 8.
		 */
		default Function1<Function8<O, B, C, D, E, F, G, H, I>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i) -> apply(a, b, c, d, e, f, g, h, i);
		}

		/**
		 * 返回柯里化函数：接受前 3 个参数，再返回接受剩余 6 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 3 parameter(s) and returning a function taking the remaining 6.
		 */
		default Function3<Function6<O, D, E, F, G, H, I>, A, B, C> curry3() {
			return (a, b, c) -> (d, e, f, g, h, i) -> apply(a, b, c, d, e, f, g, h, i);
		}

		/**
		 * 返回柯里化函数：接受前 4 个参数，再返回接受剩余 5 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 4 parameter(s) and returning a function taking the remaining 5.
		 */
		default Function4<Function5<O, E, F, G, H, I>, A, B, C, D> curry4() {
			return (a, b, c, d) -> (e, f, g, h, i) -> apply(a, b, c, d, e, f, g, h, i);
		}

		/**
		 * 返回柯里化函数：接受前 8 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 8 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function8<Function1<O, I>, A, B, C, D, E, F, G, H> curry8() {
			return (a, b, c, d, e, f, g, h) -> i -> apply(a, b, c, d, e, f, g, h, i);
		}
	}

	public static interface Function10<O, A, B, C, D, E, F, G, H, I, J> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 9 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 9.
		 */
		default Function1<Function9<O, B, C, D, E, F, G, H, I, J>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j) -> apply(a, b, c, d, e, f, g, h, i, j);
		}

		/**
		 * 返回柯里化函数：接受前 4 个参数，再返回接受剩余 6 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 4 parameter(s) and returning a function taking the remaining 6.
		 */
		default Function4<Function6<O, E, F, G, H, I, J>, A, B, C, D> curry4() {
			return (a, b, c, d) -> (e, f, g, h, i, j) -> apply(a, b, c, d, e, f, g, h, i, j);
		}

		/**
		 * 返回柯里化函数：接受前 5 个参数，再返回接受剩余 5 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 5 parameter(s) and returning a function taking the remaining 5.
		 */
		default Function5<Function5<O, F, G, H, I, J>, A, B, C, D, E> curry5() {
			return (a, b, c, d, e) -> (f, g, h, i, j) -> apply(a, b, c, d, e, f, g, h, i, j);
		}

		/**
		 * 返回柯里化函数：接受前 9 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 9 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function9<Function1<O, J>, A, B, C, D, E, F, G, H, I> curry9() {
			return (a, b, c, d, e, f, g, h, i) -> j -> apply(a, b, c, d, e, f, g, h, i, j);
		}
	}

	public static interface Function11<O, A, B, C, D, E, F, G, H, I, J, K> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 10 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 10.
		 */
		default Function1<Function10<O, B, C, D, E, F, G, H, I, J, K>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k) -> apply(a, b, c, d, e, f, g, h, i, j, k);
		}

		/**
		 * 返回柯里化函数：接受前 4 个参数，再返回接受剩余 7 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 4 parameter(s) and returning a function taking the remaining 7.
		 */
		default Function4<Function7<O, E, F, G, H, I, J, K>, A, B, C, D> curry4() {
			return (a, b, c, d) -> (e, f, g, h, i, j, k) -> apply(a, b, c, d, e, f, g, h, i, j, k);
		}

		/**
		 * 返回柯里化函数：接受前 5 个参数，再返回接受剩余 6 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 5 parameter(s) and returning a function taking the remaining 6.
		 */
		default Function5<Function6<O, F, G, H, I, J, K>, A, B, C, D, E> curry5() {
			return (a, b, c, d, e) -> (f, g, h, i, j, k) -> apply(a, b, c, d, e, f, g, h, i, j, k);
		}

		/**
		 * 返回柯里化函数：接受前 10 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 10 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function10<Function1<O, K>, A, B, C, D, E, F, G, H, I, J> curry10() {
			return (a, b, c, d, e, f, g, h, i, j) -> k -> apply(a, b, c, d, e, f, g, h, i, j, k);
		}
	}

	public static interface Function12<O, A, B, C, D, E, F, G, H, I, J, K, L> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 11 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 11.
		 */
		default Function1<Function11<O, B, C, D, E, F, G, H, I, J, K, L>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l) -> apply(a, b, c, d, e, f, g, h, i, j, k, l);
		}

		/**
		 * 返回柯里化函数：接受前 5 个参数，再返回接受剩余 7 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 5 parameter(s) and returning a function taking the remaining 7.
		 */
		default Function5<Function7<O, F, G, H, I, J, K, L>, A, B, C, D, E> curry5() {
			return (a, b, c, d, e) -> (f, g, h, i, j, k, l) -> apply(a, b, c, d, e, f, g, h, i, j, k, l);
		}

		/**
		 * 返回柯里化函数：接受前 6 个参数，再返回接受剩余 6 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 6 parameter(s) and returning a function taking the remaining 6.
		 */
		default Function6<Function6<O, G, H, I, J, K, L>, A, B, C, D, E, F> curry6() {
			return (a, b, c, d, e, f) -> (g, h, i, j, k, l) -> apply(a, b, c, d, e, f, g, h, i, j, k, l);
		}

		/**
		 * 返回柯里化函数：接受前 11 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 11 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function11<Function1<O, L>, A, B, C, D, E, F, G, H, I, J, K> curry11() {
			return (a, b, c, d, e, f, g, h, i, j, k) -> l -> apply(a, b, c, d, e, f, g, h, i, j, k, l);
		}
	}

	public static interface Function13<O, A, B, C, D, E, F, G, H, I, J, K, L, M> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 12 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 12.
		 */
		default Function1<Function12<O, B, C, D, E, F, G, H, I, J, K, L, M>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m);
		}

		/**
		 * 返回柯里化函数：接受前 5 个参数，再返回接受剩余 8 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 5 parameter(s) and returning a function taking the remaining 8.
		 */
		default Function5<Function8<O, F, G, H, I, J, K, L, M>, A, B, C, D, E> curry5() {
			return (a, b, c, d, e) -> (f, g, h, i, j, k, l, m) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m);
		}

		/**
		 * 返回柯里化函数：接受前 6 个参数，再返回接受剩余 7 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 6 parameter(s) and returning a function taking the remaining 7.
		 */
		default Function6<Function7<O, G, H, I, J, K, L, M>, A, B, C, D, E, F> curry6() {
			return (a, b, c, d, e, f) -> (g, h, i, j, k, l, m) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m);
		}

		/**
		 * 返回柯里化函数：接受前 12 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 12 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function12<Function1<O, M>, A, B, C, D, E, F, G, H, I, J, K, L> curry12() {
			return (a, b, c, d, e, f, g, h, i, j, k, l) -> m -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m);
		}
	}

	public static interface Function14<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 13 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 13.
		 */
		default Function1<Function13<O, B, C, D, E, F, G, H, I, J, K, L, M, N>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n);
		}

		/**
		 * 返回柯里化函数：接受前 6 个参数，再返回接受剩余 8 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 6 parameter(s) and returning a function taking the remaining 8.
		 */
		default Function6<Function8<O, G, H, I, J, K, L, M, N>, A, B, C, D, E, F> curry6() {
			return (a, b, c, d, e, f) -> (g, h, i, j, k, l, m, n) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n);
		}

		/**
		 * 返回柯里化函数：接受前 7 个参数，再返回接受剩余 7 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 7 parameter(s) and returning a function taking the remaining 7.
		 */
		default Function7<Function7<O, H, I, J, K, L, M, N>, A, B, C, D, E, F, G> curry7() {
			return (a, b, c, d, e, f, g) -> (h, i, j, k, l, m, n) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n);
		}

		/**
		 * 返回柯里化函数：接受前 13 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 13 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function13<Function1<O, N>, A, B, C, D, E, F, G, H, I, J, K, L, M> curry13() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m) -> n -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n);
		}
	}

	public static interface Function15<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 14 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 14.
		 */
		default Function1<Function14<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
		}

		/**
		 * 返回柯里化函数：接受前 6 个参数，再返回接受剩余 9 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 6 parameter(s) and returning a function taking the remaining 9.
		 */
		default Function6<Function9<O, G, H, I, J, K, L, M, N, P>, A, B, C, D, E, F> curry6() {
			return (a, b, c, d, e, f) -> (g, h, i, j, k, l, m, n, o) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
		}

		/**
		 * 返回柯里化函数：接受前 7 个参数，再返回接受剩余 8 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 7 parameter(s) and returning a function taking the remaining 8.
		 */
		default Function7<Function8<O, H, I, J, K, L, M, N, P>, A, B, C, D, E, F, G> curry7() {
			return (a, b, c, d, e, f, g) -> (h, i, j, k, l, m, n, o) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
		}

		/**
		 * 返回柯里化函数：接受前 14 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 14 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function14<Function1<O, P>, A, B, C, D, E, F, G, H, I, J, K, L, M, N> curry14() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n) -> o -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
		}
	}

	public static interface Function16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 15 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 15.
		 */
		default Function1<Function15<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
		}

		/**
		 * 返回柯里化函数：接受前 7 个参数，再返回接受剩余 9 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 7 parameter(s) and returning a function taking the remaining 9.
		 */
		default Function7<Function9<O, H, I, J, K, L, M, N, P, Q>, A, B, C, D, E, F, G> curry7() {
			return (a, b, c, d, e, f, g) -> (h, i, j, k, l, m, n, o, p) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
		}

		/**
		 * 返回柯里化函数：接受前 8 个参数，再返回接受剩余 8 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 8 parameter(s) and returning a function taking the remaining 8.
		 */
		default Function8<Function8<O, I, J, K, L, M, N, P, Q>, A, B, C, D, E, F, G, H> curry8() {
			return (a, b, c, d, e, f, g, h) -> (i, j, k, l, m, n, o, p) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
		}

		/**
		 * 返回柯里化函数：接受前 15 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 15 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function15<Function1<O, Q>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> curry15() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) -> p -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
		}
	}

	public static interface Function17<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 16 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 16.
		 */
		default Function1<Function16<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r);
		}

		/**
		 * 返回柯里化函数：接受前 7 个参数，再返回接受剩余 10 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 7 parameter(s) and returning a function taking the remaining 10.
		 */
		default Function7<Function10<O, H, I, J, K, L, M, N, P, Q, R>, A, B, C, D, E, F, G> curry7() {
			return (a, b, c, d, e, f, g) -> (h, i, j, k, l, m, n, o, p, r) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r);
		}

		/**
		 * 返回柯里化函数：接受前 8 个参数，再返回接受剩余 9 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 8 parameter(s) and returning a function taking the remaining 9.
		 */
		default Function8<Function9<O, I, J, K, L, M, N, P, Q, R>, A, B, C, D, E, F, G, H> curry8() {
			return (a, b, c, d, e, f, g, h) -> (i, j, k, l, m, n, o, p, r) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r);
		}

		/**
		 * 返回柯里化函数：接受前 16 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 16 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function16<Function1<O, R>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> curry16() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) -> r -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r);
		}
	}

	public static interface Function18<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 17 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 17.
		 */
		default Function1<Function17<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s);
		}

		/**
		 * 返回柯里化函数：接受前 8 个参数，再返回接受剩余 10 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 8 parameter(s) and returning a function taking the remaining 10.
		 */
		default Function8<Function10<O, I, J, K, L, M, N, P, Q, R, S>, A, B, C, D, E, F, G, H> curry8() {
			return (a, b, c, d, e, f, g, h) -> (i, j, k, l, m, n, o, p, r, s) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s);
		}

		/**
		 * 返回柯里化函数：接受前 9 个参数，再返回接受剩余 9 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 9 parameter(s) and returning a function taking the remaining 9.
		 */
		default Function9<Function9<O, J, K, L, M, N, P, Q, R, S>, A, B, C, D, E, F, G, H, I> curry9() {
			return (a, b, c, d, e, f, g, h, i) -> (j, k, l, m, n, o, p, r, s) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s);
		}

		/**
		 * 返回柯里化函数：接受前 17 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 17 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function17<Function1<O, S>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R> curry17() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r) -> s -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s);
		}
	}

	public static interface Function19<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 18 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 18.
		 */
		default Function1<Function18<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t);
		}

		/**
		 * 返回柯里化函数：接受前 8 个参数，再返回接受剩余 11 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 8 parameter(s) and returning a function taking the remaining 11.
		 */
		default Function8<Function11<O, I, J, K, L, M, N, P, Q, R, S, T>, A, B, C, D, E, F, G, H> curry8() {
			return (a, b, c, d, e, f, g, h) -> (i, j, k, l, m, n, o, p, r, s, t) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t);
		}

		/**
		 * 返回柯里化函数：接受前 9 个参数，再返回接受剩余 10 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 9 parameter(s) and returning a function taking the remaining 10.
		 */
		default Function9<Function10<O, J, K, L, M, N, P, Q, R, S, T>, A, B, C, D, E, F, G, H, I> curry9() {
			return (a, b, c, d, e, f, g, h, i) -> (j, k, l, m, n, o, p, r, s, t) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t);
		}

		/**
		 * 返回柯里化函数：接受前 18 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 18 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function18<Function1<O, T>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S> curry18() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s) -> t -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t);
		}
	}

	public static interface Function20<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 19 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 19.
		 */
		default Function1<Function19<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u);
		}

		/**
		 * 返回柯里化函数：接受前 9 个参数，再返回接受剩余 11 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 9 parameter(s) and returning a function taking the remaining 11.
		 */
		default Function9<Function11<O, J, K, L, M, N, P, Q, R, S, T, U>, A, B, C, D, E, F, G, H, I> curry9() {
			return (a, b, c, d, e, f, g, h, i) -> (j, k, l, m, n, o, p, r, s, t, u) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u);
		}

		/**
		 * 返回柯里化函数：接受前 10 个参数，再返回接受剩余 10 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 10 parameter(s) and returning a function taking the remaining 10.
		 */
		default Function10<Function10<O, K, L, M, N, P, Q, R, S, T, U>, A, B, C, D, E, F, G, H, I, J> curry10() {
			return (a, b, c, d, e, f, g, h, i, j) -> (k, l, m, n, o, p, r, s, t, u) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u);
		}

		/**
		 * 返回柯里化函数：接受前 19 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 19 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function19<Function1<O, U>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T> curry19() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t) -> u -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u);
		}
	}

	public static interface Function21<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 20 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 20.
		 */
		default Function1<Function20<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v);
		}

		/**
		 * 返回柯里化函数：接受前 9 个参数，再返回接受剩余 12 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 9 parameter(s) and returning a function taking the remaining 12.
		 */
		default Function9<Function12<O, J, K, L, M, N, P, Q, R, S, T, U, V>, A, B, C, D, E, F, G, H, I> curry9() {
			return (a, b, c, d, e, f, g, h, i) -> (j, k, l, m, n, o, p, r, s, t, u, v) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v);
		}

		/**
		 * 返回柯里化函数：接受前 10 个参数，再返回接受剩余 11 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 10 parameter(s) and returning a function taking the remaining 11.
		 */
		default Function10<Function11<O, K, L, M, N, P, Q, R, S, T, U, V>, A, B, C, D, E, F, G, H, I, J> curry10() {
			return (a, b, c, d, e, f, g, h, i, j) -> (k, l, m, n, o, p, r, s, t, u, v) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v);
		}

		/**
		 * 返回柯里化函数：接受前 20 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 20 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function20<Function1<O, V>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U> curry20() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u) -> v -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v);
		}
	}

	public static interface Function22<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 21 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 21.
		 */
		default Function1<Function21<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w);
		}

		/**
		 * 返回柯里化函数：接受前 10 个参数，再返回接受剩余 12 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 10 parameter(s) and returning a function taking the remaining 12.
		 */
		default Function10<Function12<O, K, L, M, N, P, Q, R, S, T, U, V, W>, A, B, C, D, E, F, G, H, I, J> curry10() {
			return (a, b, c, d, e, f, g, h, i, j) -> (k, l, m, n, o, p, r, s, t, u, v, w) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w);
		}

		/**
		 * 返回柯里化函数：接受前 11 个参数，再返回接受剩余 11 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 11 parameter(s) and returning a function taking the remaining 11.
		 */
		default Function11<Function11<O, L, M, N, P, Q, R, S, T, U, V, W>, A, B, C, D, E, F, G, H, I, J, K> curry11() {
			return (a, b, c, d, e, f, g, h, i, j, k) -> (l, m, n, o, p, r, s, t, u, v, w) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w);
		}

		/**
		 * 返回柯里化函数：接受前 21 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 21 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function21<Function1<O, W>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V> curry21() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v) -> w -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w);
		}
	}

	public static interface Function23<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 22 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 22.
		 */
		default Function1<Function22<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x);
		}

		/**
		 * 返回柯里化函数：接受前 10 个参数，再返回接受剩余 13 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 10 parameter(s) and returning a function taking the remaining 13.
		 */
		default Function10<Function13<O, K, L, M, N, P, Q, R, S, T, U, V, W, X>, A, B, C, D, E, F, G, H, I, J> curry10() {
			return (a, b, c, d, e, f, g, h, i, j) -> (k, l, m, n, o, p, r, s, t, u, v, w, x) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x);
		}

		/**
		 * 返回柯里化函数：接受前 11 个参数，再返回接受剩余 12 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 11 parameter(s) and returning a function taking the remaining 12.
		 */
		default Function11<Function12<O, L, M, N, P, Q, R, S, T, U, V, W, X>, A, B, C, D, E, F, G, H, I, J, K> curry11() {
			return (a, b, c, d, e, f, g, h, i, j, k) -> (l, m, n, o, p, r, s, t, u, v, w, x) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x);
		}

		/**
		 * 返回柯里化函数：接受前 22 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 22 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function22<Function1<O, X>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W> curry22() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w) -> x -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x);
		}
	}

	public static interface Function24<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 23 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 23.
		 */
		default Function1<Function23<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y);
		}

		/**
		 * 返回柯里化函数：接受前 11 个参数，再返回接受剩余 13 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 11 parameter(s) and returning a function taking the remaining 13.
		 */
		default Function11<Function13<O, L, M, N, P, Q, R, S, T, U, V, W, X, Y>, A, B, C, D, E, F, G, H, I, J, K> curry11() {
			return (a, b, c, d, e, f, g, h, i, j, k) -> (l, m, n, o, p, r, s, t, u, v, w, x, y) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y);
		}

		/**
		 * 返回柯里化函数：接受前 12 个参数，再返回接受剩余 12 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 12 parameter(s) and returning a function taking the remaining 12.
		 */
		default Function12<Function12<O, M, N, P, Q, R, S, T, U, V, W, X, Y>, A, B, C, D, E, F, G, H, I, J, K, L> curry12() {
			return (a, b, c, d, e, f, g, h, i, j, k, l) -> (m, n, o, p, r, s, t, u, v, w, x, y) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y);
		}

		/**
		 * 返回柯里化函数：接受前 23 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 23 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function23<Function1<O, Y>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X> curry23() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x) -> y -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y);
		}
	}

	public static interface Function25<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 24 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 24.
		 */
		default Function1<Function24<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z);
		}

		/**
		 * 返回柯里化函数：接受前 11 个参数，再返回接受剩余 14 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 11 parameter(s) and returning a function taking the remaining 14.
		 */
		default Function11<Function14<O, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z>, A, B, C, D, E, F, G, H, I, J, K> curry11() {
			return (a, b, c, d, e, f, g, h, i, j, k) -> (l, m, n, o, p, r, s, t, u, v, w, x, y, z) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z);
		}

		/**
		 * 返回柯里化函数：接受前 12 个参数，再返回接受剩余 13 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 12 parameter(s) and returning a function taking the remaining 13.
		 */
		default Function12<Function13<O, M, N, P, Q, R, S, T, U, V, W, X, Y, Z>, A, B, C, D, E, F, G, H, I, J, K, L> curry12() {
			return (a, b, c, d, e, f, g, h, i, j, k, l) -> (m, n, o, p, r, s, t, u, v, w, x, y, z) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z);
		}

		/**
		 * 返回柯里化函数：接受前 24 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 24 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function24<Function1<O, Z>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y> curry24() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y) -> z -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z);
		}
	}

	public static interface Function26<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 25 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 25.
		 */
		default Function1<Function25<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa);
		}

		/**
		 * 返回柯里化函数：接受前 12 个参数，再返回接受剩余 14 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 12 parameter(s) and returning a function taking the remaining 14.
		 */
		default Function12<Function14<O, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA>, A, B, C, D, E, F, G, H, I, J, K, L> curry12() {
			return (a, b, c, d, e, f, g, h, i, j, k, l) -> (m, n, o, p, r, s, t, u, v, w, x, y, z, aa) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa);
		}

		/**
		 * 返回柯里化函数：接受前 13 个参数，再返回接受剩余 13 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 13 parameter(s) and returning a function taking the remaining 13.
		 */
		default Function13<Function13<O, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA>, A, B, C, D, E, F, G, H, I, J, K, L, M> curry13() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m) -> (n, o, p, r, s, t, u, v, w, x, y, z, aa) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa);
		}

		/**
		 * 返回柯里化函数：接受前 25 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 25 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function25<Function1<O, AA>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z> curry25() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z) -> aa -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa);
		}
	}

	public static interface Function27<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 26 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 26.
		 */
		default Function1<Function26<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab);
		}

		/**
		 * 返回柯里化函数：接受前 12 个参数，再返回接受剩余 15 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 12 parameter(s) and returning a function taking the remaining 15.
		 */
		default Function12<Function15<O, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB>, A, B, C, D, E, F, G, H, I, J, K, L> curry12() {
			return (a, b, c, d, e, f, g, h, i, j, k, l) -> (m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab);
		}

		/**
		 * 返回柯里化函数：接受前 13 个参数，再返回接受剩余 14 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 13 parameter(s) and returning a function taking the remaining 14.
		 */
		default Function13<Function14<O, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB>, A, B, C, D, E, F, G, H, I, J, K, L, M> curry13() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m) -> (n, o, p, r, s, t, u, v, w, x, y, z, aa, ab) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab);
		}

		/**
		 * 返回柯里化函数：接受前 26 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 26 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function26<Function1<O, AB>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA> curry26() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa) -> ab -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab);
		}
	}

	public static interface Function28<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 27 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 27.
		 */
		default Function1<Function27<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac);
		}

		/**
		 * 返回柯里化函数：接受前 13 个参数，再返回接受剩余 15 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 13 parameter(s) and returning a function taking the remaining 15.
		 */
		default Function13<Function15<O, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC>, A, B, C, D, E, F, G, H, I, J, K, L, M> curry13() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m) -> (n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac);
		}

		/**
		 * 返回柯里化函数：接受前 14 个参数，再返回接受剩余 14 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 14 parameter(s) and returning a function taking the remaining 14.
		 */
		default Function14<Function14<O, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC>, A, B, C, D, E, F, G, H, I, J, K, L, M, N> curry14() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n) -> (o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac);
		}

		/**
		 * 返回柯里化函数：接受前 27 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 27 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function27<Function1<O, AC>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB> curry27() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab) -> ac -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac);
		}
	}

	public static interface Function29<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 28 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 28.
		 */
		default Function1<Function28<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad);
		}

		/**
		 * 返回柯里化函数：接受前 13 个参数，再返回接受剩余 16 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 13 parameter(s) and returning a function taking the remaining 16.
		 */
		default Function13<Function16<O, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD>, A, B, C, D, E, F, G, H, I, J, K, L, M> curry13() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m) -> (n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad);
		}

		/**
		 * 返回柯里化函数：接受前 14 个参数，再返回接受剩余 15 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 14 parameter(s) and returning a function taking the remaining 15.
		 */
		default Function14<Function15<O, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD>, A, B, C, D, E, F, G, H, I, J, K, L, M, N> curry14() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n) -> (o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad);
		}

		/**
		 * 返回柯里化函数：接受前 28 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 28 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function28<Function1<O, AD>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC> curry28() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac) -> ad -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad);
		}
	}

	public static interface Function30<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 29 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 29.
		 */
		default Function1<Function29<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae);
		}

		/**
		 * 返回柯里化函数：接受前 14 个参数，再返回接受剩余 16 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 14 parameter(s) and returning a function taking the remaining 16.
		 */
		default Function14<Function16<O, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE>, A, B, C, D, E, F, G, H, I, J, K, L, M, N> curry14() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n) -> (o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae);
		}

		/**
		 * 返回柯里化函数：接受前 15 个参数，再返回接受剩余 15 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 15 parameter(s) and returning a function taking the remaining 15.
		 */
		default Function15<Function15<O, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> curry15() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) -> (p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae);
		}

		/**
		 * 返回柯里化函数：接受前 29 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 29 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function29<Function1<O, AE>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD> curry29() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad) -> ae -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae);
		}
	}

	public static interface Function31<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae, AF af);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 30 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 30.
		 */
		default Function1<Function30<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af);
		}

		/**
		 * 返回柯里化函数：接受前 14 个参数，再返回接受剩余 17 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 14 parameter(s) and returning a function taking the remaining 17.
		 */
		default Function14<Function17<O, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF>, A, B, C, D, E, F, G, H, I, J, K, L, M, N> curry14() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n) -> (o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af);
		}

		/**
		 * 返回柯里化函数：接受前 15 个参数，再返回接受剩余 16 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 15 parameter(s) and returning a function taking the remaining 16.
		 */
		default Function15<Function16<O, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> curry15() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) -> (p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af);
		}

		/**
		 * 返回柯里化函数：接受前 30 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 30 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function30<Function1<O, AF>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE> curry30() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae) -> af -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af);
		}
	}

	public static interface Function32<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae, AF af, AG ag);

		/**
		 * 返回柯里化函数：接受前 1 个参数，再返回接受剩余 31 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 1 parameter(s) and returning a function taking the remaining 31.
		 */
		default Function1<Function31<O, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG>, A> curry1() {
			return a -> (b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af, ag) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af, ag);
		}

		/**
		 * 返回柯里化函数：接受前 15 个参数，再返回接受剩余 17 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 15 parameter(s) and returning a function taking the remaining 17.
		 */
		default Function15<Function17<O, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> curry15() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o) -> (p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af, ag) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af, ag);
		}

		/**
		 * 返回柯里化函数：接受前 16 个参数，再返回接受剩余 16 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 16 parameter(s) and returning a function taking the remaining 16.
		 */
		default Function16<Function16<O, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> curry16() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) -> (r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af, ag) -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af, ag);
		}

		/**
		 * 返回柯里化函数：接受前 31 个参数，再返回接受剩余 1 个参数的函数。
		 * <p>
		 * Return a curried function taking the first 31 parameter(s) and returning a function taking the remaining 1.
		 */
		default Function31<Function1<O, AG>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF> curry31() {
			return (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af) -> ag -> apply(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, r, s, t, u, v, w, x, y, z, aa, ab, ac, ad, ae, af, ag);
		}
	}

	/**
	 * 包含对象和索引的条目记录，用于应用函子构建过程中追踪参数位置。
	 * <p>
	 * Item record containing an object and its index, used for tracking parameter positions during applicative functor building.
	 *
	 * @param <T> 条目类型 / the item type
	 */
	public static record Item<T>(T obj, int index) {
	}

	/**
	 * 构建结果记录，包含条目数组、消费函数和参数数量。
	 * <p>
	 * Build result record containing the item array, consumer function and parameter count.
	 *
	 * @param <T> 条目类型 / the item type
	 * @param <O> 输出类型 / the output type
	 */
	public static record BuildResult<T, O>(Item<T>[] obj, Function<BuiltParams, O> consumer,int parcount) {
	}

	/**
	 * 已构建参数接口，提供按索引访问参数的能力。
	 * <p>
	 * Built parameters interface providing the ability to access parameters by index.
	 */
	public static interface BuiltParams{

		/**
		 * 获取指定索引的原始参数对象。
		 * <p>
		 * Get the raw parameter object at the specified index.
		 *
		 * @param params 参数索引 / the parameter index
		 * @return 原始参数对象 / the raw parameter object
		 */
		public Object getRaw(int params);

		/**
		 * 获取指定索引的参数，自动转换类型。
		 * <p>
		 * Get the parameter at the specified index with automatic type casting.
		 *
		 * @param <T> 目标类型 / the target type
		 * @param params 参数索引 / the parameter index
		 * @return 类型转换后的参数 / the type-cast parameter
		 */
		@SuppressWarnings("unchecked")
		default <T> T get(int params) {
			return (T)getRaw(params);
		}
	}

	/**
	 * 可应用接口，表示可以参与应用函子构建的元素。
	 * <p>
	 * Applicatable interface representing elements that can participate in applicative functor building.
	 *
	 * @param <T> 自身类型 / the self type
	 * @param <A> 值类型 / the value type
	 */
	public static interface Applicatable<T, A> {
		/**
		 * 获取当前条目，默认返回自身。
		 * <p>
		 * Get the current item, defaults to returning self.
		 *
		 * @return 当前条目 / the current item
		 */
		@SuppressWarnings("unchecked")
		default T getItem() {
			return (T) this;
		}
	}

	/**
	 * 0参数应用函子构建器，作为链式构建的起点。
	 * <p>
	 * Zero-parameter applicative functor builder, serving as the starting point of the chain building.
	 *
	 * @param <T> 可应用元素类型 / the applicatable element type
	 */
	public static class Applicative0<T> {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static final Applicative0 EMPTY = new Applicative0<>(new Item[0]);
		@SuppressWarnings("unchecked")
		public static <T> Applicative0<T> getInstance() {
			return EMPTY;
		}
		private final Item<T>[] item;

		private Applicative0(Item<T>[] item) {
			super();
			this.item = item;
		}

		public <A> Applicative1<T, A> add(Applicatable<T, A> item) {
			return new Applicative1<T, A>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 0)));
		}
		public Applicative0<T> decorator(Applicatable<T, ?> item) {
			return new Applicative0<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public BuildResult<T, Object> apply(Runnable onClose) {
			return new BuildResult<T, Object>(item, t -> null,0);
		}
	}

	// 1
	public static class Applicative1<T, A> {
		private final Item<T>[] item;

		private Applicative1(Item<T>[] item) {
			this.item = item;
		}

		public <B> Applicative2<T, A, B> add(Applicatable<T, B> item) {
			return new Applicative2<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 1)));
		}

		public Applicative1<T, A> decorator(Applicatable<T, ?> item) {
			return new Applicative1<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function1<RESULT, A> func) {
			return new BuildResult<>(item, o -> func.apply(o.get(0)),1);
		}
	}

	public static class Applicative2<T, A, B> {
		private final Item<T>[] item;

		private Applicative2(Item<T>[] item) {
			this.item = item;
		}

		public <C> Applicative3<T, A, B, C> add(Applicatable<T, C> item) {
			return new Applicative3<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 2)));
		}

		public Applicative2<T, A, B> decorator(Applicatable<T, ?> item) {
			return new Applicative2<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function2<RESULT, A, B> func) {
			return new BuildResult<>(item, o -> func.apply(o.get(0), o.get(1)),2);
		}
	}

	public static class Applicative3<T, A, B, C> {
		private final Item<T>[] item;

		private Applicative3(Item<T>[] item) {
			this.item = item;
		}

		public <D> Applicative4<T, A, B, C, D> add(Applicatable<T, D> item) {
			return new Applicative4<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 3)));
		}

		public Applicative3<T, A, B, C> decorator(Applicatable<T, ?> item) {
			return new Applicative3<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function3<RESULT, A, B, C> func) {
			return new BuildResult<>(item, o -> func.apply(o.get(0), o.get(1), o.get(2)),3);
		}
	}

	public static class Applicative4<T, A, B, C, D> {
		private final Item<T>[] item;

		private Applicative4(Item<T>[] item) {
			this.item = item;
		}

		public <E> Applicative5<T, A, B, C, D, E> add(Applicatable<T, E> item) {
			return new Applicative5<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 4)));
		}

		public Applicative4<T, A, B, C, D> decorator(Applicatable<T, ?> item) {
			return new Applicative4<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function4<RESULT, A, B, C, D> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3)),4);
		}
	}

	public static class Applicative5<T, A, B, C, D, E> {
		private final Item<T>[] item;

		private Applicative5(Item<T>[] item) {
			this.item = item;
		}

		public <F> Applicative6<T, A, B, C, D, E, F> add(Applicatable<T, F> item) {
			return new Applicative6<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 5)));
		}

		public Applicative5<T, A, B, C, D, E> decorator(Applicatable<T, ?> item) {
			return new Applicative5<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function5<RESULT, A, B, C, D, E> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4)),5);
		}
	}

	public static class Applicative6<T, A, B, C, D, E, F> {
		private final Item<T>[] item;

		private Applicative6(Item<T>[] item) {
			this.item = item;
		}

		public <G> Applicative7<T, A, B, C, D, E, F, G> add(Applicatable<T, G> item) {
			return new Applicative7<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 6)));
		}

		public Applicative6<T, A, B, C, D, E, F> decorator(Applicatable<T, ?> item) {
			return new Applicative6<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function6<RESULT, A, B, C, D, E, F> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5)),6);
		}
	}

	public static class Applicative7<T, A, B, C, D, E, F, G> {
		private final Item<T>[] item;

		private Applicative7(Item<T>[] item) {
			this.item = item;
		}

		public <H> Applicative8<T, A, B, C, D, E, F, G, H> add(Applicatable<T, H> item) {
			return new Applicative8<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 7)));
		}

		public Applicative7<T, A, B, C, D, E, F, G> decorator(Applicatable<T, ?> item) {
			return new Applicative7<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function7<RESULT, A, B, C, D, E, F, G> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6)),7);
		}
	}

	public static class Applicative8<T, A, B, C, D, E, F, G, H> {
		private final Item<T>[] item;

		private Applicative8(Item<T>[] item) {
			this.item = item;
		}

		public <I> Applicative9<T, A, B, C, D, E, F, G, H, I> add(Applicatable<T, I> item) {
			return new Applicative9<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 8)));
		}

		public Applicative8<T, A, B, C, D, E, F, G, H> decorator(Applicatable<T, ?> item) {
			return new Applicative8<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function8<RESULT, A, B, C, D, E, F, G, H> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7)),8);
		}
	}

	public static class Applicative9<T, A, B, C, D, E, F, G, H, I> {
		private final Item<T>[] item;

		private Applicative9(Item<T>[] item) {
			this.item = item;
		}

		public <J> Applicative10<T, A, B, C, D, E, F, G, H, I, J> add(Applicatable<T, J> item) {
			return new Applicative10<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 9)));
		}

		public Applicative9<T, A, B, C, D, E, F, G, H, I> decorator(Applicatable<T, ?> item) {
			return new Applicative9<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function9<RESULT, A, B, C, D, E, F, G, H, I> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7), 
				o.get(8)),9);
		}
	}

	public static class Applicative10<T, A, B, C, D, E, F, G, H, I, J> {
		private final Item<T>[] item;

		private Applicative10(Item<T>[] item) {
			this.item = item;
		}

		public <K> Applicative11<T, A, B, C, D, E, F, G, H, I, J, K> add(Applicatable<T, K> item) {
			return new Applicative11<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), 10)));
		}

		public Applicative10<T, A, B, C, D, E, F, G, H, I, J> decorator(Applicatable<T, ?> item) {
			return new Applicative10<>(ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function10<RESULT, A, B, C, D, E, F, G, H, I, J> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7), 
				o.get(8), o.get(9)),10);
		}
	}

	public static class Applicative11<T, A, B, C, D, E, F, G, H, I, J, K> {
		private final Item<T>[] item;

		private Applicative11(Item<T>[] item) {
			this.item = item;
		}

		public <L> Applicative12<T, A, B, C, D, E, F, G, H, I, J, K, L> add(Applicatable<T, L> item) {
			return new Applicative12<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 11)));
		}

		public Applicative11<T, A, B, C, D, E, F, G, H, I, J, K> decorator(Applicatable<T, ?> item) {
			return new Applicative11<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function11<RESULT, A, B, C, D, E, F, G, H, I, J, K> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10)),11);
		}
	}

	public static class Applicative12<T, A, B, C, D, E, F, G, H, I, J, K, L> {
		private final Item<T>[] item;

		private Applicative12(Item<T>[] item) {
			this.item = item;
		}

		public <M> Applicative13<T, A, B, C, D, E, F, G, H, I, J, K, L, M> add(Applicatable<T, M> item) {
			return new Applicative13<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 12)));
		}

		public Applicative12<T, A, B, C, D, E, F, G, H, I, J, K, L> decorator(Applicatable<T, ?> item) {
			return new Applicative12<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function12<RESULT, A, B, C, D, E, F, G, H, I, J, K, L> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11)),12);
		}
	}

	public static class Applicative13<T, A, B, C, D, E, F, G, H, I, J, K, L, M> {
		private final Item<T>[] item;

		private Applicative13(Item<T>[] item) {
			this.item = item;
		}

		public <N> Applicative14<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N> add(Applicatable<T, N> item) {
			return new Applicative14<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 13)));
		}

		public Applicative13<T, A, B, C, D, E, F, G, H, I, J, K, L, M> decorator(Applicatable<T, ?> item) {
			return new Applicative13<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function13<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12)),13);
		}
	}

	public static class Applicative14<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N> {
		private final Item<T>[] item;

		private Applicative14(Item<T>[] item) {
			this.item = item;
		}

		public <O> Applicative15<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> add(Applicatable<T, O> item) {
			return new Applicative15<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 14)));
		}

		public Applicative14<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N> decorator(Applicatable<T, ?> item) {
			return new Applicative14<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function14<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13)),14);
		}
	}

	public static class Applicative15<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> {
		private final Item<T>[] item;

		private Applicative15(Item<T>[] item) {
			this.item = item;
		}

		public <P> Applicative16<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> add(Applicatable<T, P> item) {
			return new Applicative16<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 15)));
		}

		public Applicative15<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> decorator(Applicatable<T, ?> item) {
			return new Applicative15<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function15<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14)),15);
		}
	}

	public static class Applicative16<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> {
		private final Item<T>[] item;

		private Applicative16(Item<T>[] item) {
			this.item = item;
		}

		public <Q> Applicative17<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> add(Applicatable<T, Q> item) {
			return new Applicative17<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 16)));
		}

		public Applicative16<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> decorator(Applicatable<T, ?> item) {
			return new Applicative16<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function16<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15)),16);
		}
	}

	public static class Applicative17<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> {
		private final Item<T>[] item;

		private Applicative17(Item<T>[] item) {
			this.item = item;
		}

		public <R> Applicative18<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> add(Applicatable<T, R> item) {
			return new Applicative18<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 17)));
		}

		public Applicative17<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> decorator(Applicatable<T, ?> item) {
			return new Applicative17<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function17<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16)),17);
		}
	}

	public static class Applicative18<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> {
		private final Item<T>[] item;

		private Applicative18(Item<T>[] item) {
			this.item = item;
		}

		public <S> Applicative19<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> add(Applicatable<T, S> item) {
			return new Applicative19<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 18)));
		}

		public Applicative18<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> decorator(Applicatable<T, ?> item) {
			return new Applicative18<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function18<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17)),18);
		}
	}

	public static class Applicative19<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> {
		private final Item<T>[] item;

		private Applicative19(Item<T>[] item) {
			this.item = item;
		}

		public <U> Applicative20<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U> add(Applicatable<T, U> item) {
			return new Applicative20<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 19)));
		}

		public Applicative19<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> decorator(Applicatable<T, ?> item) {
			return new Applicative19<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function19<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18)),19);
		}
	}

	public static class Applicative20<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U> {
		private final Item<T>[] item;

		private Applicative20(Item<T>[] item) {
			this.item = item;
		}

		public <V> Applicative21<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V> add(Applicatable<T, V> item) {
			return new Applicative21<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 20)));
		}

		public Applicative20<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U> decorator(Applicatable<T, ?> item) {
			return new Applicative20<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function20<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19)),20);
		}
	}

	public static class Applicative21<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V> {
		private final Item<T>[] item;

		private Applicative21(Item<T>[] item) {
			this.item = item;
		}

		public <W> Applicative22<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W> add(Applicatable<T, W> item) {
			return new Applicative22<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 21)));
		}

		public Applicative21<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V> decorator(Applicatable<T, ?> item) {
			return new Applicative21<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function21<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20)),21);
		}
	}

	public static class Applicative22<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W> {
		private final Item<T>[] item;

		private Applicative22(Item<T>[] item) {
			this.item = item;
		}

		public <X> Applicative23<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X> add(Applicatable<T, X> item) {
			return new Applicative23<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 22)));
		}

		public Applicative22<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W> decorator(Applicatable<T, ?> item) {
			return new Applicative22<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function22<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21)),22);
		}
	}

	public static class Applicative23<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X> {
		private final Item<T>[] item;

		private Applicative23(Item<T>[] item) {
			this.item = item;
		}

		public <Y> Applicative24<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y> add(Applicatable<T, Y> item) {
			return new Applicative24<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 23)));
		}

		public Applicative23<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X> decorator(Applicatable<T, ?> item) {
			return new Applicative23<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function23<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22)),23);
		}
	}

	public static class Applicative24<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y> {
		private final Item<T>[] item;

		private Applicative24(Item<T>[] item) {
			this.item = item;
		}
		public <Z> Applicative25<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z> add(Applicatable<T, Z> item) {
			return new Applicative25<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 24)));
		}
		public Applicative24<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y> decorator(Applicatable<T, ?> item) {
			return new Applicative24<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function24<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23)),24);
		}
	}

	public static class Applicative25<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z> {
		private final Item<T>[] item;

		private Applicative25(Item<T>[] item) {
			this.item = item;
		}

		public <AA> Applicative26<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA> add(Applicatable<T, AA> item) {
			return new Applicative26<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 25)));
		}

		public Applicative25<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z> decorator(Applicatable<T, ?> item) {
			return new Applicative25<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function25<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23),
				o.get(24)),25);
		}
	}

	public static class Applicative26<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA> {
		private final Item<T>[] item;

		private Applicative26(Item<T>[] item) {
			this.item = item;
		}

		public <AB> Applicative27<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB> add(Applicatable<T, AB> item) {
			return new Applicative27<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 26)));
		}

		public Applicative26<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA> decorator(Applicatable<T, ?> item) {
			return new Applicative26<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function26<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23),
				o.get(24), o.get(25)),26);
		}
	}

	public static class Applicative27<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB> {
		private final Item<T>[] item;

		private Applicative27(Item<T>[] item) {
			this.item = item;
		}

		public <AC> Applicative28<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC> add(Applicatable<T, AC> item) {
			return new Applicative28<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 27)));
		}

		public Applicative27<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB> decorator(Applicatable<T, ?> item) {
			return new Applicative27<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function27<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23),
				o.get(24), o.get(25), o.get(26)),27);
		}
	}

	public static class Applicative28<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC> {
		private final Item<T>[] item;

		private Applicative28(Item<T>[] item) {
			this.item = item;
		}

		public <AD> Applicative29<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD> add(Applicatable<T, AD> item) {
			return new Applicative29<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 28)));
		}

		public Applicative28<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC> decorator(Applicatable<T, ?> item) {
			return new Applicative28<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function28<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23),
				o.get(24), o.get(25), o.get(26), o.get(27)),28);
		}
	}

	public static class Applicative29<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD> {
		private final Item<T>[] item;

		private Applicative29(Item<T>[] item) {
			this.item = item;
		}

		public <AE> Applicative30<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE> add(Applicatable<T, AE> item) {
			return new Applicative30<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 29)));
		}

		public Applicative29<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD> decorator(Applicatable<T, ?> item) {
			return new Applicative29<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function29<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23),
				o.get(24), o.get(25), o.get(26), o.get(27),
				o.get(28)),29);
		}
	}

	public static class Applicative30<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE> {
		private final Item<T>[] item;

		private Applicative30(Item<T>[] item) {
			this.item = item;
		}

		public <AF> Applicative31<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF> add(Applicatable<T, AF> item) {
			return new Applicative31<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 30)));
		}

		public Applicative30<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE> decorator(Applicatable<T, ?> item) {
			return new Applicative30<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function30<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23),
				o.get(24), o.get(25), o.get(26), o.get(27),
				o.get(28), o.get(29)),30);
		}
	}

	public static class Applicative31<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF> {
		private final Item<T>[] item;

		private Applicative31(Item<T>[] item) {
			this.item = item;
		}

		public <AG> Applicative32<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG> add(Applicatable<T, AG> item) {
			return new Applicative32<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 31)));
		}

		public Applicative31<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF> decorator(Applicatable<T, ?> item) {
			return new Applicative31<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function31<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23),
				o.get(24), o.get(25), o.get(26), o.get(27),
				o.get(28), o.get(29), o.get(30)),31);
		}
	}

	public static class Applicative32<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG> {
		private final Item<T>[] item;

		private Applicative32(Item<T>[] item) {
			this.item = item;
		}

		public Applicative32<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG> decorator(Applicatable<T, ?> item) {
			return new Applicative32<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), -1)));
		}

		public <RESULT> BuildResult<T, RESULT> apply(Function32<RESULT, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG> func) {
			return new BuildResult<>(item, o -> func.apply(
				o.get(0), o.get(1), o.get(2), o.get(3),
				o.get(4), o.get(5), o.get(6), o.get(7),
				o.get(8), o.get(9), o.get(10), o.get(11),
				o.get(12), o.get(13), o.get(14), o.get(15),
				o.get(16), o.get(17), o.get(18), o.get(19),
				o.get(20), o.get(21), o.get(22), o.get(23),
				o.get(24), o.get(25), o.get(26), o.get(27),
				o.get(28), o.get(29), o.get(30), o.get(31)),32);
		}
	}

	/**
	 * 使用构建器函数创建应用函子构建结果。从空的Applicative0开始链式构建。
	 * <p>
	 * Create an applicative functor build result using a builder function. Starts chain building from an empty Applicative0.
	 *
	 * @param <T> 可应用元素类型 / the applicatable element type
	 * @param <O> 输出类型 / the output type
	 * @param builder 构建器函数 / the builder function
	 * @return 构建结果 / the build result
	 */
	public static <T,O> BuildResult<T, O> build(Function<Applicative0<T>, BuildResult<T, O>> builder) {
		return builder.apply(Applicative0.getInstance());
	}
}
