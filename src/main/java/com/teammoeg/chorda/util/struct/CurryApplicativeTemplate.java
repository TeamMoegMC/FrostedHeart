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

package com.teammoeg.chorda.util.struct;

import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder.SetterAndGetter;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Applicative0;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.BuildResult;

public class CurryApplicativeTemplate {
	private CurryApplicativeTemplate() {

	}

	public static interface Function1<O, A> {
		O apply(A a);
	}

	public static interface Function2<O, A, B> {
		O apply(A a, B b);
	}

	public static interface Function3<O, A, B, C> {
		O apply(A a, B b, C c);
	}

	public static interface Function4<O, A, B, C, D> {
		O apply(A a, B b, C c, D d);
	}

	public static interface Function5<O, A, B, C, D, E> {
		O apply(A a, B b, C c, D d, E e);
	}

	public static interface Function6<O, A, B, C, D, E, F> {
		O apply(A a, B b, C c, D d, E e, F f);
	}

	public static interface Function7<O, A, B, C, D, E, F, G> {
		O apply(A a, B b, C c, D d, E e, F f, G g);
	}

	public static interface Function8<O, A, B, C, D, E, F, G, H> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h);
	}

	public static interface Function9<O, A, B, C, D, E, F, G, H, I> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i);
	}

	public static interface Function10<O, A, B, C, D, E, F, G, H, I, J> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
	}

	public static interface Function11<O, A, B, C, D, E, F, G, H, I, J, K> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k);
	}

	public static interface Function12<O, A, B, C, D, E, F, G, H, I, J, K, L> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l);
	}

	public static interface Function13<O, A, B, C, D, E, F, G, H, I, J, K, L, M> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m);
	}

	public static interface Function14<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n);
	}

	public static interface Function15<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o);
	}

	public static interface Function16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p);
	}

	public static interface Function17<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r);
	}

	public static interface Function18<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s);
	}

	public static interface Function19<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t);
	}

	public static interface Function20<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u);
	}

	public static interface Function21<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v);
	}

	public static interface Function22<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w);
	}

	public static interface Function23<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x);
	}

	public static interface Function24<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y);
	}

	public static interface Function25<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z);
	}

	public static interface Function26<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa);
	}

	public static interface Function27<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab);
	}

	public static interface Function28<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac);
	}

	public static interface Function29<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad);
	}

	public static interface Function30<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae);
	}

	public static interface Function31<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae, AF af);
	}

	public static interface Function32<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG> {
		O apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae, AF af, AG ag);
	}

	public static record Item<T>(T obj, int index) {
	}

	public static record BuildResult<T, O>(Item<T>[] obj, Function<BuiltParams, O> consumer,int parcount) {
	}
	public static interface BuiltParams{

		public Object getRaw(int params);
		@SuppressWarnings("unchecked")
		default <T> T get(int params) {
			return (T)getRaw(params);
		}
	}
	public static interface Applicatable<T extends Applicatable<T, ?>, A> {
		@SuppressWarnings("unchecked")
		default T getItem() {
			return (T) this;
		}
	}

	public static class Applicative0<T extends Applicatable<T, ?>> {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static final Applicative0 EMPTY = new Applicative0<>(new Item[0]);
		@SuppressWarnings("unchecked")
		public static <T extends Applicatable<T, ?>> Applicative0<T> getInstance() {
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
	public static class Applicative1<T extends Applicatable<T, ?>, A> {
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

	public static class Applicative2<T extends Applicatable<T, ?>, A, B> {
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

	public static class Applicative3<T extends Applicatable<T, ?>, A, B, C> {
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

	public static class Applicative4<T extends Applicatable<T, ?>, A, B, C, D> {
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

	public static class Applicative5<T extends Applicatable<T, ?>, A, B, C, D, E> {
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

	public static class Applicative6<T extends Applicatable<T, ?>, A, B, C, D, E, F> {
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

	public static class Applicative7<T extends Applicatable<T, ?>, A, B, C, D, E, F, G> {
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

	public static class Applicative8<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H> {
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

	public static class Applicative9<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I> {
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
				o.get(4), o.get(5), o.get(6), o.get(7), o.get(8)),9);
		}
	}

	public static class Applicative10<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J> {
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
				o.get(4), o.get(5), o.get(6), o.get(7), o.get(8), o.get(9)),10);
		}
	}

	public static class Applicative11<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K> {
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

	public static class Applicative12<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L> {
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

	public static class Applicative13<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M> {
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

	public static class Applicative14<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N> {
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

	public static class Applicative15<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> {
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

	public static class Applicative16<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> {
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

	public static class Applicative17<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q> {
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

	public static class Applicative18<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R> {
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

	public static class Applicative19<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S> {
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

	public static class Applicative20<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U> {
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

	public static class Applicative21<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V> {
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

	public static class Applicative22<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W> {
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

	public static class Applicative23<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X> {
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

	public static class Applicative24<T extends Applicatable<T, ?>, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y> {
		private final Item<T>[] item;

		private Applicative24(Item<T>[] item) {
			this.item = item;
		}
/*
		public <Z> Applicative25<T, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, U, V, W, X, Y, Z> add(Applicatable<T, Z> item) {
			return new Applicative25<>(
				ArrayUtils.add(this.item, new Item<T>(item.getItem(), 24)));
		}
*/
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
	public static <T extends Applicatable<T, ?>,O> BuildResult<T, O> build(Function<Applicative0<T>, BuildResult<T, O>> builder) {
		return builder.apply(Applicative0.getInstance());
	}
}
