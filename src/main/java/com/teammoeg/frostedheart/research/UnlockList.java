package com.teammoeg.frostedheart.research;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

import java.util.*;
import java.util.function.Consumer;

public abstract class UnlockList<T> implements Iterable<T> {
    Set<T> s = new HashSet<>();

    public UnlockList() {

    }

    public UnlockList(ListNBT nbt) {
        this();
        load(nbt);
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
}
