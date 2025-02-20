package com.teammoeg.frostedheart.content.research.gui.editor;

import java.util.function.Function;

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
	public static enum Applicative0 {
		INSTANCE;

		public <O,A> Applicative1<O, A> add(EditorItemFactory<A> editor, Function<O, A> getter) {
			EditorDialog.EditorDialogPrototype<O> dialog = new EditorDialog.EditorDialogPrototype<O>();
			dialog.add(editor, getter);
			return new Applicative1<O, A>(dialog);
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

		public Editor<O> apply(Constructor1<O, A> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0))));
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

		public Editor<O> apply(Constructor2<O, A, B> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1))));
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

		public Editor<O> apply(Constructor3<O, A, B, C> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2))));
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

		public Editor<O> apply(Constructor4<O, A, B, C, D> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3))));
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

		public Editor<O> apply(Constructor5<O, A, B, C, D, E> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4))));
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

		public Editor<O> apply(Constructor6<O, A, B, C, D, E, F> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5))));
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

		public Editor<O> apply(Constructor7<O, A, B, C, D, E, F, G> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6))));
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

		public Editor<O> apply(Constructor8<O, A, B, C, D, E, F, G, H> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7))));
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

		public Editor<O> apply(Constructor9<O, A, B, C, D, E, F, G, H, I> func) {
			return (p, l, v, c) -> dialog.create(p, l, v,
				o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7), (I) o.get(8))));
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

		public Editor<O> apply(Constructor10<O, A, B, C, D, E, F, G, H, I, J> func) {
			return (p, l, v, c) -> dialog.create(p, l, v,
				o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7), (I) o.get(8), (J) o.get(9))));
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

		public Editor<O> apply(Constructor11<O, A, B, C, D, E, F, G, H, I, J, K> func) {
			return (p, l, v, c) -> dialog.create(p, l, v,
				o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7), (I) o.get(8), (J) o.get(9), (K) o.get(10))));
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

		public Editor<O> apply(Constructor12<O, A, B, C, D, E, F, G, H, I, J, K, L> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(
				func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7), (I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11))));
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

		public Editor<O> apply(Constructor13<O, A, B, C, D, E, F, G, H, I, J, K, L, M> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
				(I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12))));
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

		public Editor<O> apply(Constructor14<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
				(I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13))));
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

		public Editor<O> apply(Constructor15<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
				(I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14))));
		}
	}

	public static class Applicative16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> {
		private final EditorDialog.EditorDialogPrototype<O> dialog;

		private Applicative16(EditorDialog.EditorDialogPrototype<O> dialog) {
			this.dialog = dialog;
		}

		public Editor<O> apply(Constructor16<O, A, B, C, D, E, F, G, H, I, J, K, L, M, N, P, Q> func) {
			return (p, l, v, c) -> dialog.create(p, l, v, o -> c.accept(func.create((A) o.get(0), (B) o.get(1), (C) o.get(2), (D) o.get(3), (E) o.get(4), (F) o.get(5), (G) o.get(6), (H) o.get(7),
				(I) o.get(8), (J) o.get(9), (K) o.get(10), (L) o.get(11), (M) o.get(12), (N) o.get(13), (P) o.get(14), (Q) o.get(15))));
		}
	}
	public static Applicative0 builder() {
		return Applicative0.INSTANCE;
	}
}
