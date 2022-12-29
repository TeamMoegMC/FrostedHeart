/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

public abstract class UnlockList<T> implements Iterable<T> {
    Set<T> s = new HashSet<>();

    public UnlockList() {

    }

    public UnlockList(ListNBT nbt) {
        this();
        load(nbt);
    }
    public void clear() {
    	s.clear();
    }
    public boolean has(T key) {
        return s.contains(key);
    }

    public void add(T key) {
        s.add(key);
    }

    public void addAll(Collection<T> key) {
        s.addAll(key);
    }

    public abstract String getString(T item);

    public abstract T getObject(String s);

    public ListNBT serialize() {
        ListNBT ln = new ListNBT();
        for (T t : s)
            ln.add(StringNBT.valueOf(getString(t)));
        return ln;
    }

    public void remove(T key) {
        s.remove(key);
    }

    public void removeAll(Collection<T> key) {
        s.removeAll(key);
    }

    public void load(ListNBT nbt) {
        for (INBT in : nbt) {
            s.add(getObject(in.getString()));
        }
    }

    @Override
    public Iterator<T> iterator() {
        return s.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        s.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return s.spliterator();
    }
    public void reload() {
    	Set<T> ns=new HashSet<>(s);
    	s.clear();
    	ns.stream().map(this::getString).map(this::getObject).forEach(s::add);
    }
}
