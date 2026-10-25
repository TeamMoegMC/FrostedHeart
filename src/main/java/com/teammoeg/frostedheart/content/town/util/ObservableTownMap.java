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

package com.teammoeg.frostedheart.content.town.util;

import java.util.AbstractMap;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Insertion-ordered map that binds value listeners and reports committed changes.
 * AbstractMap's views and Map's default mutation methods all use put/remove or the
 * wrapped entry iterator, so there is no second, unobserved mutation path.
 * Callbacks only collect changes or bind listeners; gameplay notifications belong
 * to the town operation after its related facts have been updated.
 */
public final class ObservableTownMap<K, V> extends AbstractMap<K, V> {
    private final Map<K, V> values = new LinkedHashMap<>();
    private Consumer<K> onChange = key -> {};
    private Consumer<V> onAttach = value -> {};
    private Consumer<V> onDetach = value -> {};
    private final Set<Entry<K, V>> entries = new AbstractSet<>() {
        @Override public int size() { return values.size(); }
        @Override public void clear() { ObservableTownMap.this.clear(); }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            Iterator<Entry<K, V>> delegate = values.entrySet().iterator();
            return new Iterator<>() {
                private Entry<K, V> current;

                @Override public boolean hasNext() { return delegate.hasNext(); }

                @Override
                public Entry<K, V> next() {
                    current = delegate.next();
                    K key = current.getKey();
                    return new SimpleEntry<>(current) {
                        @Override
                        public V setValue(V value) {
                            V previous = ObservableTownMap.this.put(key, value);
                            super.setValue(value);
                            return previous;
                        }
                    };
                }

                @Override
                public void remove() {
                    // Delegate first, so an invalid/double remove cannot notify.
                    delegate.remove();
                    removed(current.getKey(), current.getValue());
                }
            };
        }
    };

    public void setOnChange(Consumer<K> listener) {
        onChange = Objects.requireNonNull(listener);
    }

    public void setOnAttach(Consumer<V> listener) {
        onAttach = Objects.requireNonNull(listener);
    }

    public void setOnDetach(Consumer<V> listener) {
        onDetach = Objects.requireNonNull(listener);
    }

    @Override public int size() { return values.size(); }
    @Override public boolean containsKey(Object key) { return values.containsKey(key); }
    @Override public boolean containsValue(Object value) { return values.containsValue(value); }
    @Override public V get(Object key) { return values.get(key); }
    @Override public Set<Entry<K, V>> entrySet() { return entries; }

    // Hot resident/building loops read values directly. Only the entry view
    // needs a setValue wrapper; ordinary iteration must not allocate per value.
    @Override
    public Collection<V> values() {
        return new AbstractCollection<>() {
            @Override public int size() { return values.size(); }
            @Override public boolean contains(Object value) { return values.containsValue(value); }
            @Override public void clear() { ObservableTownMap.this.clear(); }
            @Override public Iterator<V> iterator() { return projectingIterator(Entry::getValue); }
        };
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<>() {
            @Override public int size() { return values.size(); }
            @Override public boolean contains(Object key) { return values.containsKey(key); }
            @Override public void clear() { ObservableTownMap.this.clear(); }
            @Override public Iterator<K> iterator() { return projectingIterator(Entry::getKey); }
        };
    }

    private <T> Iterator<T> projectingIterator(Function<Entry<K, V>, T> projection) {
        var delegate = values.entrySet().iterator();
        return new Iterator<>() {
            private Entry<K, V> current;
            @Override public boolean hasNext() { return delegate.hasNext(); }
            @Override public T next() { current = delegate.next(); return projection.apply(current); }
            @Override public void remove() { delegate.remove(); removed(current.getKey(), current.getValue()); }
        };
    }

    @Override
    public void forEach(java.util.function.BiConsumer<? super K, ? super V> action) {
        values.forEach(action);
    }

    @Override
    public V put(K key, V value) {
        V previous = values.put(key, value);
        if (previous != value) {
            if (previous != null) onDetach.accept(previous);
            if (value != null) onAttach.accept(value);
        }
        onChange.accept(key);
        return previous;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> additions) {
        additions.forEach(this::put);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (!values.containsKey(key)) return null;
        V previous = values.remove(key);
        removed((K) key, previous);
        return previous;
    }

    @Override
    public void clear() {
        if (values.isEmpty()) return;
        var previous = new ArrayList<>(values.entrySet());
        values.clear();
        for (var entry : previous) removed(entry.getKey(), entry.getValue());
    }

    private void removed(K key, V value) {
        if (value != null) onDetach.accept(value);
        onChange.accept(key);
    }
}
