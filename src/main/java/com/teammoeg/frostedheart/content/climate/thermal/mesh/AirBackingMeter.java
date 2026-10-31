/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/** Counts unique retained array backing and conservative container backing. */
public final class AirBackingMeter implements AutoCloseable {
    private static final int INITIAL_ENTRIES = 32;
    private static final long WORKSPACE_HEADER_BYTES = 128;
    private static final boolean DEEP_CONTAINERS = Boolean.parseBoolean(System.getenv("FH_THERMAL_DEEP_MEMORY_PROFILE"));
    public static boolean debugEnabled() { return DEEP_CONTAINERS; }
    /** Capacity survives removals; size alone cannot measure an open-addressed table. */
    private static final ClassValue<Field[]> FASTUTIL_ARRAYS = new ClassValue<>() {
        @Override protected Field[] computeValue(Class<?> type) {
            var fields = new java.util.ArrayList<Field>();
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (!current.getName().startsWith("it.unimi.dsi.fastutil.")) continue;
                for (Field field : current.getDeclaredFields())
                    if (!Modifier.isStatic(field.getModifiers()) && field.getType().isArray() && field.trySetAccessible())
                        fields.add(field);
            }
            return fields.toArray(Field[]::new);
        }
    };
    private final SeenObjects seen;
    private final ThermalMemoryBudget.Reservation workspace;
    private final boolean summaries;
    private long bytes;

    public AirBackingMeter() { this(INITIAL_ENTRIES); }

    public AirBackingMeter(int expectedEntries) {
        this(Math.max(INITIAL_ENTRIES, expectedEntries), null, false);
    }

    /** Reuse local capacity summaries; sharing between summaries may be counted twice. */
    public AirBackingMeter(boolean summaries) { this(INITIAL_ENTRIES, null, summaries); }

    public boolean summaries() { return summaries; }

    /** Coordinator-owned scratch; capacity remains charged between independent scans. */
    public AirBackingMeter(ThermalMemoryBudget budget) {
        this(INITIAL_ENTRIES, budget.reserveCompletion(WORKSPACE_HEADER_BYTES
                + arrayPayloadBytes(HashCommon.arraySize(INITIAL_ENTRIES, Hash.DEFAULT_LOAD_FACTOR) + 1, 8)), true);
    }

    private AirBackingMeter(int expectedEntries, ThermalMemoryBudget.Reservation workspace, boolean summaries) {
        this.workspace = workspace;
        this.summaries = summaries;
        try { seen = new SeenObjects(expectedEntries, workspace); }
        catch (RuntimeException | Error failure) {
            if (workspace != null) workspace.close();
            throw failure;
        }
    }

    private static final class SeenObjects extends ReferenceOpenHashSet<Object> {
        private final ThermalMemoryBudget.Reservation workspace;

        SeenObjects(int expectedEntries, ThermalMemoryBudget.Reservation workspace) {
            super(expectedEntries, Hash.DEFAULT_LOAD_FACTOR);
            this.workspace = workspace;
        }

        long backingBytes() { return WORKSPACE_HEADER_BYTES + shallowArrayBytes(key); }

        @Override protected void rehash(int capacity) {
            if (workspace == null) { super.rehash(capacity); return; }
            workspace.beforeAllocation(arrayPayloadBytes(capacity + 1, 8));
            try { super.rehash(capacity); }
            finally { workspace.completeResize(backingBytes()); }
        }
    }

    public int entries() { return seen.size(); }

    public long bytes() { return bytes; }

    public long workspaceBytes() { return seen.backingBytes(); }

    /** Release every visited object even when a scan failed; keep only empty capacity. */
    public void clear() { seen.clear(); bytes = 0; }

    @Override public void close() {
        clear();
        if (workspace != null) workspace.close();
    }

    public static long arrayPayloadBytes(int count, int width) {
        if (count < 0 || width <= 0) throw new IllegalArgumentException("array dimensions are invalid");
        return aligned(Math.addExact(16L, Math.multiplyExact((long) count, width)));
    }

    /** One array's physical capacity, without walking references or allocating a visited table. */
    public static long shallowArrayBytes(Object value) {
        if (value == null) return 0;
        Class<?> element = value.getClass().getComponentType();
        if (element == null) throw new IllegalArgumentException("array backing is required");
        int width = element == byte.class || element == boolean.class ? 1
                : element == short.class || element == char.class ? 2
                : element == int.class || element == float.class ? 4 : 8;
        return arrayPayloadBytes(Array.getLength(value), width);
    }

    /** Capacity only. The caller accounts for its own values, not their reachable object graphs. */
    public static long containerBackingBytes(Object owner, int maximumEntries, int bytesPerEntry) {
        if (owner == null) return 0;
        if (!DEEP_CONTAINERS) return containerPayloadBytes(maximumEntries, bytesPerEntry);
        var arrays = FASTUTIL_ARRAYS.get(owner.getClass());
        if (arrays.length == 0) return containerPayloadBytes(maximumEntries, bytesPerEntry);
        long count = 128;
        for (Field field : arrays) {
            try { count = Math.addExact(count, shallowArrayBytes(field.get(owner))); }
            catch (IllegalAccessException unavailable) {
                return Math.addExact(count, containerPayloadBytes(maximumEntries, bytesPerEntry));
            }
        }
        return count;
    }

    /** Visit an immutable owner once in this inventory; its backing is charged separately. */
    public boolean visit(Object value) { return value != null && seen.add(value); }

    public boolean objectOnce(Object value, long headerBytes) {
        if (!visit(value)) return false;
        bytes = Math.addExact(bytes, aligned(headerBytes));
        return true;
    }

    public void array(Object value) {
        if (!visit(value)) return;
        Class<?> type = value.getClass();
        if (!type.isArray()) throw new IllegalArgumentException("array backing is required");
        Class<?> element = type.getComponentType();
        int width = element == byte.class || element == boolean.class ? 1
                : element == short.class || element == char.class ? 2
                : element == int.class || element == float.class ? 4 : 8;
        int count = Array.getLength(value);
        bytes = Math.addExact(bytes, arrayPayloadBytes(count, width));
        // Component[], Page[], etc. cannot contain arrays. Object[], array-of-array,
        // Cloneable[] and Serializable[] can, and still require recursive accounting.
        if (element.isArray() || element.isAssignableFrom(Object[].class)) {
            for (Object nested : (Object[]) value)
                if (nested != null && nested.getClass().isArray()) array(nested);
        }
    }

    public void containerEntries(int maximumEntries, int bytesPerEntry) {
        bytes = Math.addExact(bytes, containerPayloadBytes(maximumEntries, bytesPerEntry));
    }

    private static long containerPayloadBytes(int maximumEntries, int bytesPerEntry) {
        if (maximumEntries < 0 || bytesPerEntry < 0) throw new IllegalArgumentException("container size is invalid");
        int capacity = 16;
        while (capacity < maximumEntries * 2L) capacity = Math.multiplyExact(capacity, 2);
        return aligned(32L + Math.multiplyExact((long) capacity, bytesPerEntry));
    }

    /** Shared regional services own one container backing, irrespective of the number of readers. */
    public void container(Object owner, int maximumEntries, int bytesPerEntry) {
        if (owner == null || !seen.add(owner)) return;
        if (summaries || !DEEP_CONTAINERS) { containerEntries(maximumEntries, bytesPerEntry); return; }
        var arrays = FASTUTIL_ARRAYS.get(owner.getClass());
        if (arrays.length == 0) { containerEntries(maximumEntries, bytesPerEntry); return; }
        // Primitive/reference backing is exact. The instance/view allowance is
        // conservative and independent of the JVM's compressed-reference mode.
        bytes(128);
        for (Field field : arrays) {
            try { array(field.get(owner)); }
            catch (IllegalAccessException unavailable) { containerEntries(maximumEntries, bytesPerEntry); }
        }
    }

    public void bytes(long count) {
        bytes = Math.addExact(bytes, count);
    }

    private static long aligned(long value) { return (value + 7L) & ~7L; }
}
