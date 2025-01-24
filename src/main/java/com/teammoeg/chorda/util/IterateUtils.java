package com.teammoeg.chorda.util;

import java.util.Iterator;

import com.teammoeg.chorda.util.struct.MutablePair;

public class IterateUtils {
	private static abstract class BiIterator<K,V> implements Iterator<MutablePair<K,V>>{
		protected Iterator<K> first;
		protected Iterator<V> second;
		MutablePair<K,V> pair=new MutablePair<>();
		public BiIterator(Iterator<K> first, Iterator<V> second) {
			super();
			this.first = first;
			this.second = second;
		}
	}
	private static class BiAndIterator<K,V> extends BiIterator<K,V>{
		public BiAndIterator(Iterator<K> first, Iterator<V> second) {
			super(first,second);
		}

		@Override
		public boolean hasNext() {
			return first.hasNext()&&second.hasNext();
		}

		@Override
		public MutablePair<K, V> next() {
			pair.set(first.next(), second.next());
			return pair;
		}
	}
	private static class BiOrIterator<K,V> extends BiIterator<K,V>{
		public BiOrIterator(Iterator<K> first, Iterator<V> second) {
			super(first,second);
		}

		@Override
		public boolean hasNext() {
			return first.hasNext()||second.hasNext();
		}

		@Override
		public MutablePair<K, V> next() {
			pair.set(first.hasNext()?first.next():null, second.hasNext()?second.next():null);
			return pair;
		}
	}
	public static <K,V> Iterable<MutablePair<K,V>> joinAnd(Iterable<K> first,Iterable<V> second){
		return new Iterable<MutablePair<K,V>>() {
			@Override
			public Iterator<MutablePair<K, V>> iterator() {
				return new BiAndIterator<>(first.iterator(),second.iterator());
			}
		};
	}
	public static <K,V> Iterable<MutablePair<K,V>> joinOr(Iterable<K> first,Iterable<V> second){
		return new Iterable<MutablePair<K,V>>() {
			@Override
			public Iterator<MutablePair<K, V>> iterator() {
				return new BiOrIterator<>(first.iterator(),second.iterator());
			}
		};
	}
}
