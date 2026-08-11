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

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 一个会在键被新增 / 替换 / 移除时触发 {@code onChange} 回调的 {@link LinkedHashMap} 包装类。
 *
 * <p>本类<b>不持有任何脏状态</b>（脏键集由外部监听器如 {@code DataSyncCache} 维护），
 * 仅作为"自动 fire 中继"：拦截集合层面的变更入口，把被改的键转发出去。
 * 调用方只要写 {@code buildings.put(pos, b)} / {@code buildings.remove(pos)}，
 * 无需手动标记——变更会经 {@code onChange} 自动送达订阅者。</p>
 *
 * <p>拦截以下导致集合内容变化的入口，对每个被改的键调用 {@code onChange.accept(key)}：
 * <ul>
 *     <li>{@link #put(Object, Object)} — 新增或替换，键被通知</li>
 *     <li>{@link #putAll(Map)} — 对所有新增/替换键通知</li>
 *     <li>{@link #remove(Object)} — 若确实移除，键被通知</li>
 *     <li>{@link #clear()} — 对所有残留键通知</li>
 *     <li>通过 {@link #entrySet()} / {@link #keySet()} / {@link #values()} 迭代时的
 *         {@link Iterator#remove()} — 键被通知（覆盖 for 循环内删除，
 *         如 {@code checkBlocks} 中的 {@code iterator.remove()}）</li>
 * </ul>
 *
 * <p>典型用途：将 {@code TeamTownData} 的 {@code buildings} / {@code residents} 换成本类，
 * 在 {@code TeamTownData} 构造器里绑定三个回调（本类为<b>增量更新专用</b>——三个回调
 * 都是 DataSyncCache 同步链路的组成部分，单回调即可，居民模拟等外部消费者不应注册于此，
 * 居民生命周期事件由 {@code TeamTownData} 门面另行通知）：
 * <ul>
 *     <li>{@code setOnAttach}（建议<b>批量 put 之前</b>绑定）——把 {@code dataSyncCache} 自动传给
 *         每个 value，使建筑/居民对象内部字段变更能 fire 增量同步事件（layer ②）；</li>
 *     <li>{@code setOnDetach}——value 被替换/移除时解除绑定，避免僵死对象继续 fire；</li>
 *     <li>{@code setOnChange}（建议<b>批量 put 之后</b>绑定）——把被改的键转发到
 *         {@code dataSyncCache.addChanged}，仅用于集合层面脏标记（layer ①），避免加载存档时误标脏。</li>
 * </ul>
 * <pre>{@code
 * this.buildings.setOnAttach(b -> b.setChangeEventListener(this.dataSyncCache));
 * this.residents.setOnAttach(r -> r.setChangeEventListener(this.dataSyncCache));
 * this.buildings.setOnDetach(b -> b.setChangeEventListener(null));
 * this.residents.setOnDetach(r -> r.setChangeEventListener(null));
 * this.buildings.setOnChange((pos) -> this.dataSyncCache.onBuildingChange(new TownBuildingChangeEvent(this.buildings, pos)));
 * this.residents.setOnChange((uuid) -> this.dataSyncCache.onResidentChange(new TownResidentChangeEvent(this.residents, uuid)));
 * }</pre>
 * 之后任何位置的集合增删——构造器、玩家交互、每日逻辑——都会自动接管监听器接线与脏标记，
 * <b>调用方无需手动 {@code setChangeEventListener}</b>。</p>
 *
 * <p>{@code onChange} 默认是 no-op（无参构造 / codec 反序列化阶段），因此在本类被外部
 * 绑定回调之前发生的变化会被静默丢弃——这通常无害（如加载存档后的首次全量同步会覆盖）。
 * 一旦调用 {@link #setOnChange(Consumer)} 绑定，后续所有变更都会转发。</p>
 *
 * <p>限制：基于 lambda 的修改方法 {@code compute} / {@code merge} / {@code replace} /
 * {@code putIfAbsent} <b>未</b>被拦截。对 {@code buildings} / {@code residents} 而言这些不会被用到；
 * 若将来需要，再补充对应 override 即可。</p>
 */
public final class ObservableTownMap<K, V> extends LinkedHashMap<K, V> {

    private Consumer<K> onChange = k -> {};
    private Consumer<V> onAttach = v -> {};
    private Consumer<V> onDetach = v -> {};

    /** 无参构造：codec 反序列化 / 运行时默认使用，回调为 no-op（变更被静默丢弃）。 */
    public ObservableTownMap() {
    }

    /**
     * 注入 onChange 回调（如把被改的键转发到 {@code DataSyncCache.addChanged}）。
     * 非 null；传 null 会立即抛出 {@link NullPointerException}。
     */
    public void setOnChange(Consumer<K> onChange) {
        this.onChange = Objects.requireNonNull(onChange);
    }

    /**
     * 注入 onAttach 回调：每次 put / putAll 新增或替换 value 时，用此回调把外部监听器
     * （如 {@code dataSyncCache}）传给 value 对象（要求其具备 {@code setChangeEventListener}）。
     * 这样 value 内部字段变更即可经自身监听器 fire 增量同步事件（layer ②）。
     * 建议<b>在批量 put 之前</b>绑定，使反序列化得到的对象也自动接上监听器。
     * 非 null；传 null 会立即抛出 {@link NullPointerException}。
     */
    public void setOnAttach(Consumer<V> onAttach) {
        this.onAttach = Objects.requireNonNull(onAttach);
    }

    /**
     * 注入 onDetach 回调：value 被替换 / 移除 / clear 时解除其与外部监听器的绑定
     * （通常传 {@code v -> v.setChangeEventListener(null)}），
     * 避免已离开 Map 的僵死对象继续向 {@code dataSyncCache} fire。
     * 非 null；传 null 会立即抛出 {@link NullPointerException}。
     */
    public void setOnDetach(Consumer<V> onDetach) {
        this.onDetach = Objects.requireNonNull(onDetach);
    }

    private void notifyChange(K key) {
        onChange.accept(key);
    }

    private void detachAndNotify(K key, V value) {
        onDetach.accept(value);
        notifyChange(key);
    }

    @Override
    public V put(K key, V value) {
        V old = super.put(key, value);
        if (old != null) {
            onDetach.accept(old);
        }
        onAttach.accept(value);
        notifyChange(key);
        return old;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            K key = e.getKey();
            V newVal = e.getValue();
            V old = this.get(key);
            if (old != null) {
                onDetach.accept(old);
            }
            onAttach.accept(newVal);
            notifyChange(key);
        }
        super.putAll(m);
    }

    @Override
    public V remove(Object key) {
        @SuppressWarnings("unchecked")
        V old = super.remove(key);
        if (old != null) {
            detachAndNotify((K) key, old);
        }
        return old;
    }

    @Override
    public void clear() {
        if (!isEmpty()) {
            List<Map.Entry<K, V>> snapshot = new ArrayList<>(entrySet());
            for (Map.Entry<K, V> e : snapshot) {
                onDetach.accept(e.getValue());
                notifyChange(e.getKey());
            }
        }
        super.clear();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new ObservableEntrySet(super.entrySet());
    }

    @Override
    public Set<K> keySet() {
        return new ObservableKeySet();
    }

    @Override
    public Collection<V> values() {
        return new ObservableValues();
    }

    // ===== 内部包装视图：让迭代中的 remove() 也触发 notifyChange =====

    private static final class WrappedEntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        private final Iterator<Map.Entry<K, V>> delegate;
        private final BiConsumer<K, V> onEntryRemoved;
        private Map.Entry<K, V> current;

        WrappedEntryIterator(Iterator<Map.Entry<K, V>> delegate, BiConsumer<K, V> onEntryRemoved) {
            this.delegate = delegate;
            this.onEntryRemoved = onEntryRemoved;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            return current = delegate.next();
        }

        @Override
        public void remove() {
            if (current != null) {
                onEntryRemoved.accept(current.getKey(), current.getValue());
            }
            delegate.remove();
        }
    }

    private final class ObservableEntrySet implements Set<Map.Entry<K, V>> {
        private final Set<Map.Entry<K, V>> delegate;

        ObservableEntrySet(Set<Map.Entry<K, V>> delegate) {
            this.delegate = delegate;
        }

        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean contains(Object o) { return delegate.contains(o); }
        @Override public Object[] toArray() { return delegate.toArray(); }
        @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
        @Override public boolean add(Map.Entry<K, V> e) { return delegate.add(e); }
        @Override public boolean remove(Object o) { return delegate.remove(o); }
        @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
        @Override public boolean addAll(Collection<? extends Map.Entry<K, V>> c) { return delegate.addAll(c); }
        @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
        @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
        @Override public void clear() { delegate.clear(); }
        @Override public boolean equals(Object o) { return delegate.equals(o); }
        @Override public int hashCode() { return delegate.hashCode(); }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new WrappedEntryIterator<>(delegate.iterator(), ObservableTownMap.this::detachAndNotify);
        }
    }

    /** 把 keySet 的读写转发到 entrySet，使迭代删除触发 notifyChange。 */
    private final class ObservableKeySet extends AbstractSet<K> {
        @Override public int size() { return ObservableTownMap.this.size(); }
        @Override public void clear() { ObservableTownMap.this.clear(); }

        @Override
        public Iterator<K> iterator() {
            final Iterator<Map.Entry<K, V>> eit = entrySet().iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { return eit.hasNext(); }
                @Override public K next() { return eit.next().getKey(); }
                @Override public void remove() { eit.remove(); }
            };
        }
    }

    /** 把 values 的读写转发到 entrySet，使迭代删除触发 notifyChange。 */
    private final class ObservableValues extends AbstractCollection<V> {
        @Override public int size() { return ObservableTownMap.this.size(); }
        @Override public void clear() { ObservableTownMap.this.clear(); }

        @Override
        public Iterator<V> iterator() {
            final Iterator<Map.Entry<K, V>> eit = entrySet().iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { return eit.hasNext(); }
                @Override public V next() { return eit.next().getValue(); }
                @Override public void remove() { eit.remove(); }
            };
        }
    }
}
