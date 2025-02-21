package com.teammoeg.chorda.client.cui.editor;

import java.util.function.Function;

import com.mojang.datafixers.util.Unit;
import com.teammoeg.chorda.client.cui.editor.EditorDialog.EditorDialogPrototype;

public class EditorDialogBuilder {
	private EditorDialogBuilder() {

	}

	public static interface Constructor1<O, A> {
		O create(A a);
	}

	public static interface Constructor2<O, A, B> {
		O create(A a, B b);
	}

	public static interface Constructor3<O, A, B, C> {
		O create(A a, B b, C c);
	}

	public static interface Constructor4<O, A, B, C, D> {
		O create(A a, B b, C c, D d);
	}

	public static interface Constructor5<O, A, B, C, D, E> {
		O create(A a, B b, C c, D d, E e);
	}

	public static interface Constructor6<O, A, B, C, D, E, F> {
		O create(A a, B b, C c, D d, E e, F f);
	}

	public static interface Constructor7<O, A, B, C, D, E, F, G> {
		O create(A a, B b, C c, D d, E e, F f, G g);
	}

	public static interface Constructor8<O, A, B, C, D, E, F, G, H> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h);
	}

	public static interface Constructor9<O, A, B, C, D, E, F, G, H, I> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h, I i);
	}

	public static interface Constructor10<O, A, B, C, D, E, F, G, H, I, J> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
	}

	public static interface Constructor11<O, A, B, C, D, E, F, G, H, I, J, K> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k);
	}

	public static interface Constructor12<O, A, B, C, D, E, F, G, H, I, J, K, L> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l);
	}

	public static interface Constructor13<O, A, B, C, D, E, F, G, H, I, J, K, L, M> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m);
	}

	public static interface Constructor14<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n);
	}

	public static interface Constructor15<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o);
	}

	public static interface Constructor16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> {
		O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p);
	}

	public static interface Constructor17<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r);
	}

	public static interface Constructor18<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s);
	}

	public static interface Constructor19<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t);
	}

	public static interface Constructor20<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u);
	}

	public static interface Constructor21<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v);
	}

	public static interface Constructor22<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w);
	}

	public static interface Constructor23<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x);
	}

	public static interface Constructor24<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y);
	}

	public static interface Constructor25<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z);
	}

	public static interface Constructor26<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa);
	}

	public static interface Constructor27<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab);
	}

	public static interface Constructor28<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac);
	}

	public static interface Constructor29<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad);
	}

	public static interface Constructor30<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae);
	}

	public static interface Constructor31<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae, AF af);
	}

	public static interface Constructor32<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD, AE, AF, AG> {
	    O create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, P o, Q p, R r, S s, T t, U u, V v, W w, X x, Y y, Z z, AA aa, AB ab, AC ac, AD ad, AE ae, AF af, AG ag);
	}
	public static class Applicative0 {
		public static final Applicative0 INSTANCE=new Applicative0(null);
		private final EditorDialog.EditorDialogPrototype proto;
		
		public Applicative0(EditorDialogPrototype proto) {
			super();
			this.proto = proto;
		}

		public <O,A> Applicative1<O, A> add(EditorItemFactory<A> editor, Function<O, A> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = proto==null?new EditorDialog.EditorDialogPrototype<O>():new EditorDialog.EditorDialogPrototype<O>(proto);
			dialog.add(editor, getter);
			return new Applicative1<O, A>(dialog);
		} 
		public <O> Applicative0 addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = proto==null?new EditorDialog.EditorDialogPrototype<O>():new EditorDialog.EditorDialogPrototype<O>(proto);
			dialog.addAction(editor);
			return new Applicative0(dialog);
		}
		public Editor<Unit> apply(Runnable onClose){
			if(proto!=null)
				return (p, l, v, c) -> proto.create(p, l, v, o -> onClose.run()).open();
				return (p,l,v,c)->{};
		}
	}
	// 1
	public static class Applicative1<O, A> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative1(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <B> Applicative2<O, A, B> add(EditorItemFactory<B> editor, Function<O, B> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative2<O, A, B>(dialog);
		}
		public Applicative1<O, A> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative1<>(dialog);
		} 
		public Editor<O> apply(Constructor1<O, A> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0)))).open();
		}
	}

	public static class Applicative2<O, A, B> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative2(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <C> Applicative3<O, A, B, C> add(EditorItemFactory<C> editor, Function<O, C> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative3<O, A, B, C>(dialog);
		}
		public Applicative2<O, A, B> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative2<>(dialog);
		} 
		public Editor<O> apply(Constructor2<O, A, B> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1)))).open();
		}
	}

	public static class Applicative3<O, A, B, C> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative3(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <D> Applicative4<O, A, B, C, D> add(EditorItemFactory<D> editor, Function<O, D> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative4<O, A, B, C, D>(dialog);
		}
		public Applicative3<O, A,B,C> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative3<>(dialog);
		} 
		public Editor<O> apply(Constructor3<O, A, B, C> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2)))).open();
		}
	}

	public static class Applicative4<O, A, B, C, D> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative4(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <E> Applicative5<O, A, B, C, D, E> add(EditorItemFactory<E> editor, Function<O, E> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative5<O, A, B, C, D, E>(dialog);
		}
		public Applicative4<O, A,B,C,D> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative4<>(dialog);
		} 
		public Editor<O> apply(Constructor4<O, A, B, C, D> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3)))).open();
		}
	}

	public static class Applicative5<O, A, B, C, D, E> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative5(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <F> Applicative6<O, A, B, C, D, E, F> add(EditorItemFactory<F> editor, Function<O, F> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative6<O, A, B, C, D, E, F>(dialog);
		}
		public Applicative5<O, A,B,C,D,E> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative5<>(dialog);
		} 
		public Editor<O> apply(Constructor5<O, A, B, C, D, E> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4)))).open();
		}
	}

	public static class Applicative6<O, A, B, C, D, E, F> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative6(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <G> Applicative7<O, A, B, C, D, E, F, G> add(EditorItemFactory<G> editor, Function<O, G> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative7<O, A, B, C, D, E, F, G>(dialog);
		}
		public Applicative6<O, A,B,C,D,E,F> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative6<>(dialog);
		} 
		public Editor<O> apply(Constructor6<O, A, B, C, D, E, F> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5)))).open();
		}
	}

	public static class Applicative7<O, A, B, C, D, E, F, G> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative7(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <H> Applicative8<O, A, B, C, D, E, F, G, H> add(EditorItemFactory<H> editor, Function<O, H> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative8<O, A, B, C, D, E, F, G, H>(dialog);
		}
		public Applicative7<O, A,B,C,D,E,F,G> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative7<>(dialog);
		} 
		public Editor<O> apply(Constructor7<O, A, B, C, D, E, F, G> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6)))).open();
		}
	}

	public static class Applicative8<O, A, B, C, D, E, F, G, H> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative8(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <I> Applicative9<O, A, B, C, D, E, F, G, H, I> add(EditorItemFactory<I> editor, Function<O, I> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative9<O, A, B, C, D, E, F, G, H, I>(dialog);
		}
		public Applicative8<O, A,B,C,D,E,F,G,H> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative8<>(dialog);
		} 
		public Editor<O> apply(Constructor8<O, A, B, C, D, E, F, G, H> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7)))).open();
		}
	}

	public static class Applicative9<O, A, B, C, D, E, F, G, H, I> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative9(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <J> Applicative10<O, A, B, C, D, E, F, G, H, I, J> add(EditorItemFactory<J> editor, Function<O, J> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative10<O, A, B, C, D, E, F, G, H, I, J>(dialog);
		}
		public Applicative9<O, A,B,C,D,E,F,G,H,I> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative9<>(dialog);
		} 
		public Editor<O> apply(Constructor9<O, A, B, C, D, E, F, G, H, I> func) {
			return (p, l, v, c) -> dialog.create(p, l, v,
				o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7), (I) o.get(8)))).open();
		}
	}

	public static class Applicative10<O, A, B, C, D, E, F, G, H, I, J> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative10(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <K> Applicative11<O, A, B, C, D, E, F, G, H, I, J, K> add(EditorItemFactory<K> editor, Function<O, K> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative11<O, A, B, C, D, E, F, G, H, I, J, K>(dialog);
		}
		public Applicative10<O, A,B,C,D,E,F,G,H,I,J> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative10<>(dialog);
		} 
		public Editor<O> apply(Constructor10<O, A, B, C, D, E, F, G, H, I, J> func) {
			return (p, l, v, c) -> dialog.create(p, l, v,
				o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7), (I) o.get(8), (J) o.get(9)))).open();
		}
	}

	public static class Applicative11<O, A, B, C, D, E, F, G, H, I, J, K> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative11(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <L> Applicative12<O, A, B, C, D, E, F, G, H, I, J, K, L> add(EditorItemFactory<L> editor, Function<O, L> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative12<O, A, B, C, D, E, F, G, H, I, J, K, L>(dialog);
		}
		public Applicative11<O, A,B,C,D,E,F,G,H,I,J,K> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative11<>(dialog);
		} 
		public Editor<O> apply(Constructor11<O, A, B, C, D, E, F, G, H, I, J, K> func) {
			return (p, l, v, c) -> dialog.create(p, l, v,
				o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7), (I) o.get(8), (J) o.get(9), (K) o.get(10)))).open();
		}
	}

	public static class Applicative12<O, A, B, C, D, E, F, G, H, I, J, K, L> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative12(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <M> Applicative13<O, A, B, C, D, E, F, G, H, I, J, K, L, M> add(EditorItemFactory<M> editor, Function<O, M> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative13<O, A, B, C, D, E, F, G, H, I, J, K, L, M>(dialog);
		}
		public Applicative12<O, A,B,C,D,E,F,G,H,I,J,K,L> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative12<>(dialog);
		} 
		public Editor<O> apply(Constructor12<O, A, B, C, D, E, F, G, H, I, J, K, L> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(
				func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7), (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11)))).open();
		}
	}

	public static class Applicative13<O, A, B, C, D, E, F, G, H, I, J, K, L, M> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative13(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <N> Applicative14<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N> add(EditorItemFactory<N> editor, Function<O, N> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative14<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N>(dialog);
		}
		public Applicative13<O, A,B,C,D,E,F,G,H,I,J,K,L,M> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative13<>(dialog);
		} 
		public Editor<O> apply(Constructor13<O, A, B, C, D, E, F, G, H, I, J, K, L, M> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
				(I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12)))).open();
		}
	}

	public static class Applicative14<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative14(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <P> Applicative15<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> add(EditorItemFactory<P> editor, Function<O, P> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative15<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P>(dialog);
		}
		public Applicative14<O, A,B,C,D,E,F,G,H,I,J,K,L,M,N> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative14<>(dialog);
		} 
		public Editor<O> apply(Constructor14<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
				(I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13)))).open();
		}
	}

	public static class Applicative15<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative15(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public <Q> Applicative16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> add(EditorItemFactory<Q> editor, Function<O, Q> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.add(editor, getter);
			return new Applicative16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q>(dialog);
		}
		public Applicative15<O, A,B,C,D,E,F,G,H,I,J,K,L,M,N,P> addAction(EditorItemFactory<O> editor) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
			dialog.addAction(editor);
			return new Applicative15<>(dialog);
		} 
		public Editor<O> apply(Constructor15<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
				(I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14)))).open();
		}
	}

	public static class Applicative16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative16(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <R> Applicative17<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R> add(EditorItemFactory<R> editor, Function<O, R> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative17<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R>(dialog);
	    }

	    public Applicative16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.addAction(editor);
	        return new Applicative16<>(dialog);
	    }

	    public Editor<O> apply(Constructor16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15)
	        ))).open();
	    }
	}

	public static class Applicative17<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative17(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <S> Applicative18<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S> add(EditorItemFactory<S> editor, Function<O, S> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative18<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S>(dialog);
	    }

	    public Applicative17<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.addAction(editor);
	        return new Applicative17<>(dialog);
	    }

	    public Editor<O> apply(Constructor17<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16)
	        ))).open();
	    }
	}


	public static class Applicative18<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative18(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <T> Applicative19<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T> add(EditorItemFactory<T> editor, Function<O, T> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative19<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T>(dialog);
	    }

	    public Applicative18<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.addAction(editor);
	        return new Applicative18<>(dialog);
	    }

	    public Editor<O> apply(Constructor18<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17)
	        ))).open();
	    }
	}


	public static class Applicative19<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative19(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <U> Applicative20<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U> add(EditorItemFactory<U> editor, Function<O, U> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative20<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U>(dialog);
	    }

	    public Applicative19<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.addAction(editor);
	        return new Applicative19<>(dialog);
	    }

	    public Editor<O> apply(Constructor19<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18)
	        ))).open();
	    }
	}


	public static class Applicative20<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative20(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <V> Applicative21<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V> add(EditorItemFactory<V> editor, Function<O, V> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative21<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V>(dialog);
	    }

	    public Applicative20<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.addAction(editor);
	        return new Applicative20<>(dialog);
	    }

	    public Editor<O> apply(Constructor20<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19)
	        ))).open();
	    }
	}

	public static class Applicative21<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative21(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <W> Applicative22<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W> add(EditorItemFactory<W> editor, Function<O, W> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative22<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W>(dialog);
	    }

	    public Applicative21<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>(this.dialog);
	        dialog.addAction(editor);
	        return new Applicative21<>(dialog);
	    }

	    public Editor<O> apply(Constructor21<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19), (V) o.get(20)
	        ))).open();
	    }
	}

	public static class Applicative22<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative22(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <X> Applicative23<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X> add(EditorItemFactory<X> editor, Function<O, X> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative23<>(dialog);
	    }

	    public Applicative22<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.addAction(editor);
	        return this;
	    }

	    public Editor<O> apply(Constructor22<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19), (V) o.get(20), (W) o.get(21)
	        ))).open();
	    }
	}

	public static class Applicative23<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative23(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <Y> Applicative24<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y> add(EditorItemFactory<Y> editor, Function<O, Y> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative24<>(dialog);
	    }

	    public Applicative23<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.addAction(editor);
	        return this;
	    }

	    public Editor<O> apply(Constructor23<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19), (V) o.get(20), (W) o.get(21), (X) o.get(22)
	        ))).open();
	    }
	}

	public static class Applicative24<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative24(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <Z> Applicative25<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z> add(EditorItemFactory<Z> editor, Function<O, Z> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative25<>(dialog);
	    }

	    public Applicative24<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.addAction(editor);
	        return this;
	    }

	    public Editor<O> apply(Constructor24<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19), (V) o.get(20), (W) o.get(21), (X) o.get(22), (Y) o.get(23)
	        ))).open();
	    }
	}

	public static class Applicative25<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative25(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <AA> Applicative26<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA> add(EditorItemFactory<AA> editor, Function<O, AA> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative26<>(dialog);
	    }

	    public Applicative25<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.addAction(editor);
	        return this;
	    }

	    public Editor<O> apply(Constructor25<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19), (V) o.get(20), (W) o.get(21), (X) o.get(22), (Y) o.get(23), (Z) o.get(24)
	        ))).open();
	    }
	}

	public static class Applicative26<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative26(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <AB> Applicative27<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB> add(EditorItemFactory<AB> editor, Function<O, AB> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative27<>(dialog);
	    }

	    public Applicative26<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.addAction(editor);
	        return this;
	    }

	    public Editor<O> apply(Constructor26<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19), (V) o.get(20), (W) o.get(21), (X) o.get(22), (Y) o.get(23), (Z) o.get(24), (AA) o.get(25)
	        ))).open();
	    }
	}

	public static class Applicative27<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative27(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }

	    public <AC> Applicative28<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC> add(EditorItemFactory<AC> editor, Function<O, AC> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative28<>(dialog);
	    }

	    public Applicative27<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.addAction(editor);
	        return this;
	    }

	    public Editor<O> apply(Constructor27<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19), (V) o.get(20), (W) o.get(21), (X) o.get(22), (Y) o.get(23), (Z) o.get(24), (AA) o.get(25), (AB) o.get(26)
	        ))).open();
	    }
	}
	public static class Applicative28<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC> {
	    private final EditorDialog.EditorDialogPrototype<O> dialog;

	    private Applicative28(EditorDialog.EditorDialogPrototype<O> dialog) {
	        this.dialog = dialog;
	    }
/*
	    public <AD> Applicative29<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC, AD> add(EditorItemFactory<AD> editor, Function<O, AD> getter) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.add(editor, getter);
	        return new Applicative29<>(dialog);
	    }*/

	    public Applicative28<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC> addAction(EditorItemFactory<O> editor) {
	        EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<>(this.dialog);
	        dialog.addAction(editor);
	        return this;
	    }

	    public Editor<O> apply(Constructor28<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q, R, S, T, U, V, W, X, Y, Z, AA, AB, AC> func) {
	        return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create(
	            (A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
	            (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15), (R) o.get(16), (S) o.get(17), (T) o.get(18), (U) o.get(19), (V) o.get(20), (W) o.get(21), (X) o.get(22), (Y) o.get(23), (Z) o.get(24), (AA) o.get(25), (AB) o.get(26), (AC) o.get(27)
	        ))).open();
	    }
	}
	public static Applicative0 builder() {
		return Applicative0.INSTANCE;
	}
}
