/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.Arrays;

/** Allocation-free best-fit index over recyclable arena spans. */
final class ThermalFreeSpanIndex {
    static final long NO_SPAN = -1L;
    private static final int NONE = -1;

    private final Int2IntOpenHashMap byStart = new Int2IntOpenHashMap();
    private final Int2IntOpenHashMap byEnd = new Int2IntOpenHashMap();
    private int[] start = new int[16];
    private int[] length = new int[16];
    private int[] left = new int[16];
    private int[] right = new int[16];
    private int[] parent = new int[16];
    private int[] height = new int[16];
    private int[] nextFree = new int[16];
    private int root = NONE;
    private int highWater;
    private int freeHead = NONE;

    ThermalFreeSpanIndex() {
        byStart.defaultReturnValue(NONE);
        byEnd.defaultReturnValue(NONE);
        Arrays.fill(left, NONE);
        Arrays.fill(right, NONE);
        Arrays.fill(parent, NONE);
        Arrays.fill(nextFree, NONE);
    }

    long takeBestFit(int minimumLength) {
        int current = root;
        int candidate = NONE;
        while (current != NONE) {
            if (compare(current, minimumLength, NONE) >= 0) {
                candidate = current;
                current = left[current];
            } else {
                current = right[current];
            }
        }
        if (candidate == NONE) {
            return NO_SPAN;
        }
        long result = (long) length[candidate] << 32
                | start[candidate] & 0xffff_ffffL;
        remove(candidate);
        return result;
    }

    int addAndMerge(int firstSlot, int count, int allocationHighWater) {
        int mergedFirst = firstSlot;
        int mergedCount = count;
        int lower = byEnd.get(firstSlot);
        if (lower != NONE) {
            mergedFirst = start[lower];
            mergedCount = Math.addExact(mergedCount, length[lower]);
            remove(lower);
        }
        int mergedEnd = Math.addExact(mergedFirst, mergedCount);
        int higher = byStart.get(mergedEnd);
        if (higher != NONE) {
            mergedCount = Math.addExact(mergedCount, length[higher]);
            remove(higher);
            mergedEnd = Math.addExact(mergedFirst, mergedCount);
        }
        if (mergedEnd == allocationHighWater) {
            return mergedFirst;
        }
        insert(mergedFirst, mergedCount);
        return allocationHighWater;
    }

    private void insert(int firstSlot, int count) {
        int node = acquire();
        start[node] = firstSlot;
        length[node] = count;
        height[node] = 1;
        int previousStart = byStart.put(firstSlot, node);
        int previousEnd = byEnd.put(Math.addExact(firstSlot, count), node);
        if (previousStart != NONE || previousEnd != NONE) {
            throw new IllegalStateException("free arena span index is inconsistent");
        }
        if (root == NONE) {
            root = node;
            return;
        }
        int current = root;
        int owner;
        while (true) {
            owner = current;
            int comparison = compare(node, current);
            if (comparison < 0) {
                current = left[current];
                if (current == NONE) {
                    left[owner] = node;
                    break;
                }
            } else {
                current = right[current];
                if (current == NONE) {
                    right[owner] = node;
                    break;
                }
            }
        }
        parent[node] = owner;
        rebalance(owner);
    }

    private void remove(int node) {
        if (left[node] != NONE && right[node] != NONE) {
            int successor = leftmost(right[node]);
            swapPayload(node, successor);
            node = successor;
        }
        byStart.remove(start[node]);
        byEnd.remove(Math.addExact(start[node], length[node]));
        int owner = parent[node];
        int child = left[node] != NONE ? left[node] : right[node];
        replaceChild(owner, node, child);
        recycle(node);
        rebalance(owner);
    }

    private void swapPayload(int first, int second) {
        byStart.remove(start[first]);
        byEnd.remove(Math.addExact(start[first], length[first]));
        byStart.remove(start[second]);
        byEnd.remove(Math.addExact(start[second], length[second]));
        int value = start[first];
        start[first] = start[second];
        start[second] = value;
        value = length[first];
        length[first] = length[second];
        length[second] = value;
        byStart.put(start[first], first);
        byEnd.put(Math.addExact(start[first], length[first]), first);
        byStart.put(start[second], second);
        byEnd.put(Math.addExact(start[second], length[second]), second);
    }

    private void replaceChild(int owner, int oldChild, int replacement) {
        if (owner == NONE) {
            root = replacement;
        } else if (left[owner] == oldChild) {
            left[owner] = replacement;
        } else if (right[owner] == oldChild) {
            right[owner] = replacement;
        } else {
            throw new IllegalStateException("free arena AVL ownership is inconsistent");
        }
        if (replacement != NONE) {
            parent[replacement] = owner;
        }
    }

    private void rebalance(int node) {
        while (node != NONE) {
            updateHeight(node);
            int top = node;
            int balance = balance(node);
            if (balance > 1) {
                if (balance(left[node]) < 0) {
                    rotateLeft(left[node]);
                }
                top = rotateRight(node);
            } else if (balance < -1) {
                if (balance(right[node]) > 0) {
                    rotateRight(right[node]);
                }
                top = rotateLeft(node);
            }
            node = parent[top];
        }
    }

    private int rotateLeft(int node) {
        int replacement = right[node];
        int transfer = left[replacement];
        int owner = parent[node];
        replaceChild(owner, node, replacement);
        left[replacement] = node;
        parent[node] = replacement;
        right[node] = transfer;
        if (transfer != NONE) {
            parent[transfer] = node;
        }
        updateHeight(node);
        updateHeight(replacement);
        return replacement;
    }

    private int rotateRight(int node) {
        int replacement = left[node];
        int transfer = right[replacement];
        int owner = parent[node];
        replaceChild(owner, node, replacement);
        right[replacement] = node;
        parent[node] = replacement;
        left[node] = transfer;
        if (transfer != NONE) {
            parent[transfer] = node;
        }
        updateHeight(node);
        updateHeight(replacement);
        return replacement;
    }

    private int acquire() {
        if (freeHead != NONE) {
            int node = freeHead;
            freeHead = nextFree[node];
            nextFree[node] = NONE;
            return node;
        }
        ensureCapacity(highWater + 1);
        return highWater++;
    }

    private void recycle(int node) {
        start[node] = 0;
        length[node] = 0;
        left[node] = NONE;
        right[node] = NONE;
        parent[node] = NONE;
        height[node] = 0;
        nextFree[node] = freeHead;
        freeHead = node;
    }

    private void ensureCapacity(int required) {
        if (required <= start.length) {
            return;
        }
        int old = start.length;
        int capacity = Math.max(required, old + Math.max(8, old >>> 1));
        start = Arrays.copyOf(start, capacity);
        length = Arrays.copyOf(length, capacity);
        left = Arrays.copyOf(left, capacity);
        right = Arrays.copyOf(right, capacity);
        parent = Arrays.copyOf(parent, capacity);
        height = Arrays.copyOf(height, capacity);
        nextFree = Arrays.copyOf(nextFree, capacity);
        Arrays.fill(left, old, capacity, NONE);
        Arrays.fill(right, old, capacity, NONE);
        Arrays.fill(parent, old, capacity, NONE);
        Arrays.fill(nextFree, old, capacity, NONE);
    }

    private int leftmost(int node) {
        while (left[node] != NONE) {
            node = left[node];
        }
        return node;
    }

    private int compare(int first, int second) {
        int comparison = Integer.compare(length[first], length[second]);
        return comparison != 0
                ? comparison : Integer.compare(start[first], start[second]);
    }

    private int compare(int node, int targetLength, int targetStart) {
        int comparison = Integer.compare(length[node], targetLength);
        return comparison != 0
                ? comparison : Integer.compare(start[node], targetStart);
    }

    private int balance(int node) {
        return node == NONE ? 0 : height(left[node]) - height(right[node]);
    }

    private int height(int node) {
        return node == NONE ? 0 : height[node];
    }

    private void updateHeight(int node) {
        height[node] = 1 + Math.max(height(left[node]), height(right[node]));
    }
}
