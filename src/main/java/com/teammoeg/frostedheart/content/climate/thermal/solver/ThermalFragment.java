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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Objects;
import java.util.ArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirRouteValidity;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;

/** Exact persistent primitive operations owned by one topology Brick. */
public record ThermalFragment(
        AirPairs airPairs,
        MaterialContacts materialContacts,
        RoutedContacts routedContacts,
        FarBoundaries farBoundaries
) {
    public static final ThermalFragment EMPTY = new ThermalFragment(
            AirPairs.EMPTY, MaterialContacts.EMPTY,
            RoutedContacts.EMPTY, FarBoundaries.EMPTY);

    public ThermalFragment {
        Objects.requireNonNull(airPairs, "airPairs");
        Objects.requireNonNull(materialContacts, "materialContacts");
        Objects.requireNonNull(routedContacts, "routedContacts");
        Objects.requireNonNull(farBoundaries, "farBoundaries");
    }

    public boolean isEmpty() {
        return airPairs.size() == 0
                && materialContacts.size() == 0
                && routedContacts.size() == 0
                && farBoundaries.size() == 0;
    }

    public void recordBacking(AirBackingMeter meter) {
        if (meter.summaries()) {
            if (!meter.objectOnce(this, ownedBackingBytes())) return;
            for (var route : routedContacts.routes) if (route != null) route.recordBacking(meter);
            recordTraces(routedContacts.firstTraces, meter); recordTraces(routedContacts.secondTraces, meter);
            return;
        }
        if (!meter.objectOnce(this, 48)) return;
        meter.array(airPairs.first); meter.array(airPairs.second); meter.array(airPairs.conductance);
        meter.array(materialContacts.first); meter.array(materialContacts.second); meter.array(materialContacts.conductance);
        meter.array(routedContacts.first); meter.array(routedContacts.second); meter.array(routedContacts.conductance);
        meter.array(routedContacts.routes); meter.array(routedContacts.firstTraces); meter.array(routedContacts.secondTraces);
        for (var route : routedContacts.routes) if (route != null) route.recordBacking(meter);
        recordTraces(routedContacts.firstTraces, meter); recordTraces(routedContacts.secondTraces, meter);
        meter.array(farBoundaries.cell);
        meter.array(farBoundaries.patchCells); meter.array(farBoundaries.patchBlocks); meter.array(farBoundaries.patchConductances);
    }

    /** Routes and face traces are borrowed from AirRouteCompiler; only the arrays below are ours. */
    public long ownedBackingBytes() {
        if (this == EMPTY) return 0;
        return 48L + AirBackingMeter.shallowArrayBytes(airPairs.first) + AirBackingMeter.shallowArrayBytes(airPairs.second)
                + AirBackingMeter.shallowArrayBytes(airPairs.conductance)
                + AirBackingMeter.shallowArrayBytes(materialContacts.first) + AirBackingMeter.shallowArrayBytes(materialContacts.second)
                + AirBackingMeter.shallowArrayBytes(materialContacts.conductance)
                + AirBackingMeter.shallowArrayBytes(routedContacts.first) + AirBackingMeter.shallowArrayBytes(routedContacts.second)
                + AirBackingMeter.shallowArrayBytes(routedContacts.conductance) + AirBackingMeter.shallowArrayBytes(routedContacts.routes)
                + AirBackingMeter.shallowArrayBytes(routedContacts.firstTraces) + AirBackingMeter.shallowArrayBytes(routedContacts.secondTraces)
                + AirBackingMeter.shallowArrayBytes(farBoundaries.cell)
                + AirBackingMeter.shallowArrayBytes(farBoundaries.patchCells)
                + AirBackingMeter.shallowArrayBytes(farBoundaries.patchBlocks) + AirBackingMeter.shallowArrayBytes(farBoundaries.patchConductances);
    }

    private static void recordTraces(AirFaceTrace[] traces, AirBackingMeter meter) {
        if (traces == null) return;
        for (var trace : traces) if (trace != null && meter.objectOnce(trace, 40)) {
            meter.array(trace.blocks); meter.array(trace.faces);
        }
    }

    /** Buoyant Air pairs; arrays transfer ownership to this value. */
    public static final class AirPairs {
        public static final AirPairs EMPTY = new AirPairs(
                new int[0], new int[0], new double[0]);

        private final int[] first;
        private final int[] second;
        private final double[] conductance;

        public AirPairs(
                int[] first,
                int[] second,
                double[] conductance
        ) {
            int size = length(first);
            if (length(second) != size || length(conductance) != size) {
                throw new IllegalArgumentException("Air pair arrays differ");
            }
            this.first = first;
            this.second = second;
            this.conductance = conductance;
        }

        public int size() { return first.length; }
        public int first(int index) { return first[index]; }
        public int second(int index) { return second[index]; }
        public double conductance(int index) { return conductance[index]; }
    }

    /** Indirect contacts retain the component that made their route possible. */
    /** Physical unit faces forming an area's Air temperature trace; arrays transfer ownership. */
    public record AirFaceTrace(long[] blocks, byte[] faces, int offset, int size) {
        public long block(int index) { return blocks[offset + index]; }
        public byte face(int index) { return faces[offset + index]; }
    }

    public record RoutedContacts(int[] first, int[] second, double[] conductance, AirRouteValidity[] routes,
                                 AirFaceTrace[] firstTraces, AirFaceTrace[] secondTraces) {
        public RoutedContacts(int[] first, int[] second, double[] conductance, AirRouteValidity[] routes) {
            this(first, second, conductance, routes, null, null);
        }
        public static final RoutedContacts EMPTY = new RoutedContacts(new int[0], new int[0], new double[0], new AirRouteValidity[0]);
        public int size() { return first.length; }
        public int first(int index) { return first[index]; }
        public int second(int index) { return second[index]; }
        public double conductance(int index) { return conductance[index]; }
        public boolean active(int index) { return routes[index].activeForSolve(); }
        public AirFaceTrace firstTrace(int index) { return firstTraces == null ? null : firstTraces[index]; }
        public AirFaceTrace secondTrace(int index) { return secondTraces == null ? null : secondTraces[index]; }

        public static final class Builder {
            private final IntArrayList first = new IntArrayList();
            private final IntArrayList second = new IntArrayList();
            private final DoubleArrayList conductance = new DoubleArrayList();
            private final ArrayList<AirRouteValidity> routes = new ArrayList<>();
            private final ArrayList<AirFaceTrace> firstTraces = new ArrayList<>(), secondTraces = new ArrayList<>();

            public void clear() { first.clear(); second.clear(); conductance.clear(); routes.clear(); firstTraces.clear(); secondTraces.clear(); }
            public void add(int a, int b, double g, AirRouteValidity route, AirFaceTrace traceA, AirFaceTrace traceB) {
                first.add(Math.min(a, b)); second.add(Math.max(a, b)); conductance.add(g); routes.add(route);
                firstTraces.add(a <= b ? traceA : traceB);
                secondTraces.add(a <= b ? traceB : traceA);
            }
            public RoutedContacts build() {
                return first.isEmpty() ? EMPTY : new RoutedContacts(first.toIntArray(), second.toIntArray(),
                        conductance.toDoubleArray(), routes.toArray(AirRouteValidity[]::new),
                        firstTraces.toArray(AirFaceTrace[]::new), secondTraces.toArray(AirFaceTrace[]::new));
            }
        }
    }

    /**
     * Contacts owned by this Brick, in physical enumeration order.
     * Positive faces have one owner; multiple faces to one Air component
     * are combined by the local compiler before this immutable array is created.
     */
    public static final class MaterialContacts {
        public static final MaterialContacts EMPTY =
                new MaterialContacts(
                        new int[0], new int[0], new double[0]);

        private final int[] first;
        private final int[] second;
        private final double[] conductance;

        public MaterialContacts(
                int[] first,
                int[] second,
                double[] conductance
        ) {
            int size = length(first);
            if (length(second) != size || length(conductance) != size) {
                throw new IllegalArgumentException(
                        "material contact arrays differ");
            }
            this.first = first;
            this.second = second;
            this.conductance = conductance;
        }

        public int size() { return first.length; }
        public int first(int index) { return first[index]; }
        public int second(int index) { return second[index]; }
        public double conductance(int index) { return conductance[index]; }
    }

    /** Local exposed Air boundaries and captured face patches. */
    public static final class FarBoundaries {
        public static final FarBoundaries EMPTY = new FarBoundaries(
                new int[0], -1, null, null, null);

        private final int[] cell;
        private final int pageSlot;
        private final int[] patchCells;
        private final long[] patchBlocks;
        private final double[] patchConductances;

        public FarBoundaries(int[] cell, int pageSlot,
                int[] patchCells, long[] patchBlocks, double[] patchConductances) {
            int size = length(cell);
            if (size != 0 && pageSlot < 0) {
                throw new IllegalArgumentException("FarField arrays differ");
            }
            this.cell = cell;
            this.pageSlot = pageSlot;
            this.patchCells = patchCells;
            this.patchBlocks = patchBlocks;
            this.patchConductances = patchConductances;
        }

        public int size() { return cell.length; }
        public int patchCount() { return patchCells == null ? 0 : patchCells.length; }
        public int patchCell(int index) { return patchCells[index]; }
        public long patchBlock(int index) { return patchBlocks[index]; }
        public double patchConductance(int index) { return patchConductances[index]; }
        public int cell(int index) { return cell[index]; }
        public int pageSlot() { return pageSlot; }
    }

    private static int length(int[] values) {
        return Objects.requireNonNull(values, "array").length;
    }

    private static int length(double[] values) {
        return Objects.requireNonNull(values, "array").length;
    }

}
