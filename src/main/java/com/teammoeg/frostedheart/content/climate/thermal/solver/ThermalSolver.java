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

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.Arrays;
import java.util.Objects;

/**
 * Worker-owned primitive topology storage for regional numerical tasks.
 *
 * <p>Each Brick owns its contacts. Preparation reserves the columns before
 * publishing their replacement through the same fragment token.</p>
 */
public final class ThermalSolver {
    private ThermalFragment[] fragments;
    private long[] fragmentPresent;
    private int[] stateReferences;
    private double[] naturalTemperatureByPage;
    private final Int2IntOpenHashMap replacementReferenceDelta =
            new Int2IntOpenHashMap();
    private record StagedFragment(com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token, ThermalFragment next) { }
    private final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<StagedFragment> stagedFragments = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
    private int pairCount;
    private int farBoundaryCount;
    private long structuralVersion;
    private double windScale = 1.0D;
    private com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation storage;

    public void storageOwner(com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation owner) {
        storage = owner;
    }

    private static long fragmentColumnBytes(int capacity) {
        return AirBackingMeter.arrayPayloadBytes(capacity, 8)
                + AirBackingMeter.arrayPayloadBytes((capacity + 63) >>> 6, 8);
    }

    public ThermalSolver(
            ThermalCellArena arena,
            int initialFragmentCapacity,
            int initialPageCapacity
    ) {
        if (initialFragmentCapacity < 0
                || initialPageCapacity < 0) {
            throw new IllegalArgumentException("solver configuration is invalid");
        }
        Objects.requireNonNull(arena, "arena");
        int fragmentCapacity = Math.max(1, initialFragmentCapacity);
        fragments = new ThermalFragment[fragmentCapacity];
        Arrays.fill(fragments, ThermalFragment.EMPTY);
        fragmentPresent = new long[(fragmentCapacity + 63) >>> 6];
        int stateCapacity = Math.max(1, arena.highWaterMark());
        stateReferences = new int[stateCapacity];
        naturalTemperatureByPage = new double[Math.max(1, initialPageCapacity)];
        Arrays.fill(naturalTemperatureByPage, Double.NaN);
        replacementReferenceDelta.defaultReturnValue(0);
    }

    public long structuralVersion() {
        return structuralVersion;
    }

    public int nextFragmentIndex(int fromInclusive) {
        int index = nextPresent(fragmentPresent, fromInclusive, fragments.length);
        while (index >= 0 && fragment(index).isEmpty()) index = nextPresent(fragmentPresent, index + 1, fragments.length);
        return index;
    }

    public double naturalTemperatureC(int pageSlot) { return naturalTemperatureByPage[pageSlot]; }
    public double windScale() { return windScale; }

    public void recordBacking(AirBackingMeter meter) {
        meter.array(fragments); meter.array(fragmentPresent);
        meter.array(stateReferences); meter.array(naturalTemperatureByPage);
        // This scratch map can retain its largest replacement until the next plan.
        meter.container(replacementReferenceDelta, stateReferences.length, 8);
        if (meter.summaries()) meter.bytes(fragmentBackingBytes);
        else {
            for (var fragment : fragments) if (fragment != null) fragment.recordBacking(meter);
        }
    }

    private long fragmentBackingBytes;

    private int stateReferenceCount(int arenaSlot) {
        return arenaSlot >= 0 && arenaSlot < stateReferences.length
                ? stateReferences[arenaSlot]
                : 0;
    }

    public boolean references(ArenaSpan span) {
        Objects.requireNonNull(span, "span");
        int end = Math.min(span.endSlotExclusive(), stateReferences.length);
        for (int slot = span.firstSlot(); slot < end; slot++) {
            if (stateReferences[slot] != 0) {
                return true;
            }
        }
        return false;
    }

    public ThermalFragment fragment(int index) {
        var staged = stagedFragments.get(index);
        if (staged != null && staged.token.committed()) return staged.next;
        return index >= 0 && index < fragments.length
                ? fragments[index]
                : ThermalFragment.EMPTY;
    }

    public void stageFragment(int index, ThermalFragment next, com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token) {
        stagedFragments.put(index, new StagedFragment(token, next));
        if (!next.isEmpty()) updatePresence(fragmentPresent, index, true);
    }
    public void finishFragment(int index, com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token) {
        var staged = stagedFragments.get(index);
        if (staged == null || staged.token != token) return;
        if (token.committed()) installFragment(index, staged.next);
        else updatePresence(fragmentPresent, index, !fragments[index].isEmpty());
        stagedFragments.remove(index);
    }
    /** Fallible preparation-time growth; never called after commit begins. */
    public void reserveTopologyCapacity(
            int requiredFragments,
            int requiredArenaSlots,
            int requiredPageSlots
    ) {
        if (requiredFragments < 0 || requiredArenaSlots < 0
                || requiredPageSlots < 0) {
            throw new IllegalArgumentException("solver reserve sizes are invalid");
        }
        if (requiredFragments > fragments.length) {
            int old = fragments.length;
            int capacity = grownCapacity(old, requiredFragments);
            if (storage != null) storage.beforeAllocation(fragmentColumnBytes(capacity));
            fragments = Arrays.copyOf(fragments, capacity);
            Arrays.fill(fragments, old, capacity, ThermalFragment.EMPTY);
            fragmentPresent = Arrays.copyOf(
                    fragmentPresent, (capacity + 63) >>> 6);
            if (storage != null) storage.afterReplacement(fragmentColumnBytes(old));
        }
        if (requiredArenaSlots > stateReferences.length) {
            int old = stateReferences.length;
            int capacity = grownCapacity(
                    stateReferences.length, requiredArenaSlots);
            if (storage != null) storage.beforeAllocation(AirBackingMeter.arrayPayloadBytes(capacity, 4));
            stateReferences = Arrays.copyOf(stateReferences, capacity);
            if (storage != null) storage.afterReplacement(AirBackingMeter.arrayPayloadBytes(old, 4));
        }
        if (requiredPageSlots > naturalTemperatureByPage.length) {
            int old = naturalTemperatureByPage.length;
            int capacity = grownCapacity(old, requiredPageSlots);
            if (storage != null) storage.beforeAllocation(AirBackingMeter.arrayPayloadBytes(capacity, 8));
            naturalTemperatureByPage = Arrays.copyOf(
                    naturalTemperatureByPage, capacity);
            Arrays.fill(
                    naturalTemperatureByPage, old, capacity, Double.NaN);
            if (storage != null) storage.afterReplacement(AirBackingMeter.arrayPayloadBytes(old, 8));
        }
    }

    /**
     * Validates one sparse replacement against the solver's committed
     * references and returns exact post-commit work counts.
     */
    public ProjectedWork preflightReplacement(
            int[] fragmentIndexes,
            ThermalFragment[] replacements,
            ArenaSpan[] retiringSpans
    ) {
        if (fragmentIndexes.length != replacements.length) {
            throw new IllegalArgumentException("solver replacement is invalid");
        }
        replacementReferenceDelta.clear();
        int pairs = pairCount;
        int boundaries = farBoundaryCount;
        // A committed token may still be waiting for fragment cleanup.
        for (var entry : stagedFragments.int2ObjectEntrySet()) if (entry.getValue().token.committed()) {
            var old = fragments[entry.getIntKey()];
            var next = entry.getValue().next;
            pairs += pairCount(next) - pairCount(old);
            boundaries += next.farBoundaries().size() - old.farBoundaries().size();
        }
        for (int index = 0; index < fragmentIndexes.length; index++) {
            ThermalFragment old = fragment(fragmentIndexes[index]);
            ThermalFragment next = replacements[index];
            applyReferences(old, -1, false);
            applyReferences(next, 1, false);
            pairs += pairCount(next) - pairCount(old);
            boundaries += next.farBoundaries().size()
                    - old.farBoundaries().size();
        }
        for (var entry : replacementReferenceDelta.int2IntEntrySet()) {
            if (stateReferenceCount(entry.getIntKey())
                    + entry.getIntValue() < 0) {
                throw new IllegalStateException(
                        "solver reference count would underflow");
            }
        }
        for (ArenaSpan span : retiringSpans) {
            for (int slot = span.firstSlot();
                 slot < span.endSlotExclusive();
                 slot++) {
                if (stateReferenceCount(slot)
                        + replacementReferenceDelta.get(slot) != 0) {
                    throw new IllegalStateException(
                            "replacement closure still references an old span");
                }
            }
        }
        return new ProjectedWork(pairs, boundaries);
    }

    /**
     * Allocation-free prepared write. The caller has already validated every
     * endpoint generation and next reference count.
     */
    public void installFragment(int index, ThermalFragment next) {
        ThermalFragment previous = fragments[index];
        applyReferences(previous, -1, true);
        adjustCounts(previous, -1);
        fragments[index] = next;
        fragmentBackingBytes += next.ownedBackingBytes() - previous.ownedBackingBytes();
        updatePresence(fragmentPresent, index, !next.isEmpty());
        adjustCounts(next, 1);
        applyReferences(next, 1, true);
    }

    public void installNaturalTemperature(int pageSlot, double temperatureC) {
        naturalTemperatureByPage[pageSlot] = temperatureC;
    }

    public void clearNaturalTemperature(int pageSlot) {
        naturalTemperatureByPage[pageSlot] = Double.NaN;
    }

    /** Final primitive write of a prepared structural version. */
    public void finishTopologyCommit(long nextStructuralVersion) {
        structuralVersion = nextStructuralVersion;
    }

    private void adjustCounts(ThermalFragment fragment, int direction) {
        pairCount += direction * pairCount(fragment);
        farBoundaryCount += direction * fragment.farBoundaries().size();
    }

    private static int pairCount(ThermalFragment fragment) {
        return Math.addExact(Math.addExact(fragment.airPairs().size(), fragment.routedContacts().size()),
                fragment.materialContacts().size());
    }

    private void applyReferences(
            ThermalFragment fragment,
            int direction,
            boolean committed
    ) {
        ThermalFragment.AirPairs air = fragment.airPairs();
        for (int index = 0; index < air.size(); index++) {
            applyReference(air.first(index), direction, committed);
            applyReference(air.second(index), direction, committed);
        }
        ThermalFragment.MaterialContacts material =
                fragment.materialContacts();
        ThermalFragment.RoutedContacts routed = fragment.routedContacts();
        for (int index = 0; index < routed.size(); index++) {
            applyReference(routed.first(index), direction, committed);
            applyReference(routed.second(index), direction, committed);
        }
        for (int index = 0; index < material.size(); index++) {
            applyReference(material.first(index), direction, committed);
            applyReference(material.second(index), direction, committed);
        }
        ThermalFragment.FarBoundaries far = fragment.farBoundaries();
        for (int index = 0; index < far.size(); index++) {
            applyReference(far.cell(index), direction, committed);
        }
    }

    private void applyReference(int slot, int direction, boolean committed) {
        if (!committed) {
            replacementReferenceDelta.put(
                    slot, replacementReferenceDelta.get(slot) + direction);
            return;
        }
        stateReferences[slot] += direction;
    }

    private static int nextPresent(long[] bits, int start, int limit) {
        if (start < 0) {
            start = 0;
        }
        if (start >= limit) {
            return -1;
        }
        int wordIndex = start >>> 6;
        long word = bits[wordIndex] & (-1L << (start & 63));
        while (true) {
            if (word != 0L) {
                int value = wordIndex << 6 | Long.numberOfTrailingZeros(word);
                return value < limit ? value : -1;
            }
            if (++wordIndex >= bits.length) {
                return -1;
            }
            word = bits[wordIndex];
        }
    }

    private static void setBit(long[] bits, int index) {
        bits[index >>> 6] |= 1L << (index & 63);
    }

    private static void clearBit(long[] bits, int index) {
        bits[index >>> 6] &= ~(1L << (index & 63));
    }

    private static void updatePresence(
            long[] bits,
            int index,
            boolean present
    ) {
        if (present) {
            setBit(bits, index);
        } else {
            clearBit(bits, index);
        }
    }

    private static int grownCapacity(int current, int required) {
        int capacity = Math.max(1, current);
        while (capacity < required) {
            capacity = Math.addExact(capacity, Math.max(8, capacity >>> 1));
        }
        return capacity;
    }

    public record ProjectedWork(
            int pairOperations,
            int boundaryOperations
    ) {
    }

}
