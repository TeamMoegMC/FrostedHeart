/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import java.util.Arrays;

/** Reusable generation-stamped primitive tables for one Brick compilation. */
final class PrimitiveTopologyScratch {
    private PrimitiveTopologyScratch() {
    }

    static final class LongPairDouble {
        private long[] first = new long[16];
        private long[] second = new long[16];
        private double[] value = new double[16];
        private int[] tableEntry = new int[32];
        private int[] tableGeneration = new int[32];
        private int generation = 1;
        private int size;

        void reset() {
            size = 0;
            if (++generation == 0) {
                Arrays.fill(tableGeneration, 0);
                generation = 1;
            }
        }

        int add(long left, long right, double delta) {
            int index = find(left, right);
            if (index >= 0) {
                value[index] += delta;
                return index;
            }
            ensureEntryCapacity(size + 1);
            if ((size + 1) * 10 > tableEntry.length * 6) {
                rehash(tableEntry.length << 1);
            }
            index = size++;
            first[index] = left;
            second[index] = right;
            value[index] = delta;
            insert(index);
            return index;
        }

        int size() { return size; }
        long first(int index) { return first[index]; }
        long second(int index) { return second[index]; }
        double value(int index) { return value[index]; }

        private int find(long left, long right) {
            int mask = tableEntry.length - 1;
            int slot = hash(left, right) & mask;
            while (tableGeneration[slot] == generation) {
                int entry = tableEntry[slot];
                if (first[entry] == left && second[entry] == right) {
                    return entry;
                }
                slot = slot + 1 & mask;
            }
            return -1;
        }

        private void insert(int entry) {
            int mask = tableEntry.length - 1;
            int slot = hash(first[entry], second[entry]) & mask;
            while (tableGeneration[slot] == generation) {
                slot = slot + 1 & mask;
            }
            tableGeneration[slot] = generation;
            tableEntry[slot] = entry;
        }

        private void ensureEntryCapacity(int required) {
            if (required <= first.length) {
                return;
            }
            int capacity = grow(first.length, required);
            first = Arrays.copyOf(first, capacity);
            second = Arrays.copyOf(second, capacity);
            value = Arrays.copyOf(value, capacity);
        }

        private void rehash(int capacity) {
            tableEntry = new int[capacity];
            tableGeneration = new int[capacity];
            generation = 1;
            for (int entry = 0; entry < size; entry++) {
                insert(entry);
            }
        }
    }

    static final class OwnerLongInt {
        private int[] owner = new int[32];
        private long[] key = new long[32];
        private int[] value = new int[32];
        private int[] tableEntry = new int[64];
        private int[] tableGeneration = new int[64];
        private int generation = 1;
        private int size;

        void reset() {
            size = 0;
            if (++generation == 0) {
                Arrays.fill(tableGeneration, 0);
                generation = 1;
            }
        }

        void add(int entryOwner, long entryKey, int delta) {
            int index = find(entryOwner, entryKey);
            if (index >= 0) {
                value[index] = Math.addExact(value[index], delta);
                return;
            }
            ensureEntryCapacity(size + 1);
            if ((size + 1) * 10 > tableEntry.length * 6) {
                rehash(tableEntry.length << 1);
            }
            index = size++;
            owner[index] = entryOwner;
            key[index] = entryKey;
            value[index] = delta;
            insert(index);
        }

        int size() { return size; }
        int owner(int index) { return owner[index]; }
        long key(int index) { return key[index]; }
        int value(int index) { return value[index]; }

        private int find(int entryOwner, long entryKey) {
            int mask = tableEntry.length - 1;
            int slot = hash(entryOwner, entryKey) & mask;
            while (tableGeneration[slot] == generation) {
                int entry = tableEntry[slot];
                if (owner[entry] == entryOwner && key[entry] == entryKey) {
                    return entry;
                }
                slot = slot + 1 & mask;
            }
            return -1;
        }

        private void insert(int entry) {
            int mask = tableEntry.length - 1;
            int slot = hash(owner[entry], key[entry]) & mask;
            while (tableGeneration[slot] == generation) {
                slot = slot + 1 & mask;
            }
            tableGeneration[slot] = generation;
            tableEntry[slot] = entry;
        }

        private void ensureEntryCapacity(int required) {
            if (required <= owner.length) {
                return;
            }
            int capacity = grow(owner.length, required);
            owner = Arrays.copyOf(owner, capacity);
            key = Arrays.copyOf(key, capacity);
            value = Arrays.copyOf(value, capacity);
        }

        private void rehash(int capacity) {
            tableEntry = new int[capacity];
            tableGeneration = new int[capacity];
            generation = 1;
            for (int entry = 0; entry < size; entry++) {
                insert(entry);
            }
        }
    }

    private static int grow(int current, int required) {
        int capacity = Math.max(1, current);
        while (capacity < required) {
            capacity = Math.addExact(capacity, Math.max(8, capacity >>> 1));
        }
        return capacity;
    }

    private static int hash(long first, long second) {
        long mixed = first ^ Long.rotateLeft(second, 29);
        mixed ^= mixed >>> 30;
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94d049bb133111ebL;
        mixed ^= mixed >>> 31;
        return (int) mixed;
    }

    private static int hash(int owner, long key) {
        return hash(Integer.toUnsignedLong(owner), key);
    }
}
