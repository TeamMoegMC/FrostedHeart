/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

/** Unique material edges executed by one canonical owner fragment. */
public final class ThermalMaterialExecution {
    public static final ThermalMaterialExecution EMPTY =
            new ThermalMaterialExecution(
                    new long[0], new int[0], new int[0],
                    new double[0], new double[0]);

    private final long[] keys;
    private final int[] first;
    private final int[] second;
    private final double[] conductance;
    private final double[] coefficient;

    public ThermalMaterialExecution(
            long[] keys,
            int[] first,
            int[] second,
            double[] conductance,
            double[] coefficient
    ) {
        int size = keys == null ? -1 : keys.length;
        if (size < 0 || first == null || first.length != size
                || second == null || second.length != size
                || conductance == null || conductance.length != size
                || coefficient == null || coefficient.length != size) {
            throw new IllegalArgumentException(
                    "material execution arrays are invalid");
        }
        this.keys = keys;
        this.first = first;
        this.second = second;
        this.conductance = conductance;
        this.coefficient = coefficient;
    }

    public int size() { return keys.length; }
    public long key(int index) { return keys[index]; }
    public int first(int index) { return first[index]; }
    public int second(int index) { return second[index]; }
    public double conductance(int index) { return conductance[index]; }
    public double coefficient(int index) { return coefficient[index]; }
}
