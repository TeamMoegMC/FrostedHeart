package com.teammoeg.chorda.util.struct;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An optimized enummap with most common use, it has faster query speed and lower memory usage, but it's not very good at iteration<br>
 * It don't check input type as they are bounded in generics<br>
 * Consider constructing with a shared {@link FastEnumMapGenerator#create()} to speed up the construction
 * It don't allow null value, such mapping would be treated as none entry<br>
 * If Other collection feature is needed, use {@link java.util.EnumMap} instead
 * **/
public class FastEnumMap<K extends Enum<K>,V> implements Iterable<Map.Entry<K, V>>{
	/**
	 * An optimized way to produce fast enum map.
	 * */
	public static class FastEnumMapGenerator<K extends Enum<K>>{
		private K[] keys;

	    public FastEnumMapGenerator(Class<K> keyType) {
	       keys=keyType.getEnumConstants();
	    }
	    public <V> FastEnumMap<K,V> create(){
	    	return new FastEnumMap<>(keys);
	    }
	}
    /**
     * Array representation of this map.  The ith element is the value
     * to which universe[i] is currently mapped, or null if it isn't
     * mapped to anything.
     */
    private Object[] vals;
    private K[] keyType;

    /**
     * Creates an empty enum map with the specified key type.
     * Note that keyType MUST be all keys of the enum, otherwise this would cause an issue
     * This method is appearently faster.
     * @param keyType the class object of the key type for this enum map
     * @throws NullPointerException if {@code keyType} is null
     */
    public FastEnumMap(K[] keyType) {
        this.keyType = keyType;
        vals = new Object[keyType.length];
    }

    public FastEnumMap(Class<K> keyType) {
       this(keyType.getEnumConstants());
    }
    /**
     * Creates an enum map with the same key type as the specified enum
     * map, initially containing the same mappings (if any).
     *
     * @param m the enum map from which to initialize this enum map
     * @throws NullPointerException if {@code m} is null
     */
    public FastEnumMap(FastEnumMap<K, ? extends V> m) {
        //keyType = m.keyType;
        vals = m.vals.clone();
    }

    // Query Operations

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.
     *
     * @param value the value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to this value
     */
    public boolean containsValue(V value) {

        for (Object val : vals)
            if (value.equals(val))
                return true;

        return false;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified
     *            key
     */
    public boolean containsKey(K key) {
        return key!=null&&vals[key.ordinal()] != null;
    }
    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key == k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     */
    public V get(K key) {
        return (key!=null ?(V) vals[key.ordinal()] : null);
    }

    // Modification Operations

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     *
     * @return the previous value associated with specified key, or
     *     {@code null} if there was no mapping for key.  (A {@code null}
     *     return can also indicate that the map previously associated
     *     {@code null} with the specified key.)
     * @throws NullPointerException if the specified key is null
     */
    public V put(K key, V value) {
        int index = key.ordinal();
        Object oldValue = vals[index];
        vals[index] = value;
        return (V) oldValue;
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with specified key, or
     *     {@code null} if there was no entry for key.  (A {@code null}
     *     return can also indicate that the map previously associated
     *     {@code null} with the specified key.)
     */
    public V remove(K key) {
        int index = key.ordinal();
        Object oldValue = vals[index];
        vals[index] = null;
        return (V) oldValue;
    }

    // Bulk Operations

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m the mappings to be stored in this map
     * @throws NullPointerException the specified map is null, or if
     *     one or more keys in the specified map are null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        for(java.util.Map.Entry<? extends K, ? extends V> i:m.entrySet()) {
        	this.put(i.getKey(), i.getValue());
        }
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        Arrays.fill(vals, null);
    }

    private class EntryIterator implements Iterator<Map.Entry<K,V>> {
        private int index=0;
        public Map.Entry<K,V> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            return new Entry(index++);
        }
        public boolean hasNext() {
        	while(vals[index]==null&&index<vals.length) {
        		index++;
        	}
        	if(index>=vals.length)return false;
        	return true;
        }

        public void remove() {
            vals[index-1]=null;
        }

        private class Entry implements Map.Entry<K,V> {
            private int index;
            private Entry(int index) {
                this.index = index;
            }

            public K getKey() {
            	return keyType[index];
            }

            public V getValue() {
                return (V) vals[index];
            }

            public V setValue(V value) {
            	V oldValue=(V) vals[index];
                vals[index] = value;
                return oldValue;
            }

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + getEnclosingInstance().hashCode();
				result = prime * result + Objects.hash(index);
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Entry other = (Entry) obj;
				if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
					return false;
				return index == other.index;
			}

			private EntryIterator getEnclosingInstance() {
				return EntryIterator.this;
			}


        }

    }

	@Override
	public Iterator<java.util.Map.Entry<K, V>> iterator() {
		return new EntryIterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(vals);
		result = prime * result + keyType.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FastEnumMap other = (FastEnumMap) obj;
		return keyType==other.keyType && Arrays.deepEquals(vals, other.vals);
	}



}
