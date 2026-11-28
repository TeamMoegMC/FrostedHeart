/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

/**
 * One prepared heat interval borrowed by the numerical field. Its owner keeps
 * the arrays fixed until the field commits or cancels this exact interval.
 */
public interface ThermalHeatStep {
    long sequence();
    long fromTick();
    long toTick();
    int size();
    long position(int slot);
    int materialGeneration(int slot);
    int face(int slot);
    double joules(int slot);
    double pendingJoules(int slot);
    void defer(int slot, double joules);
    void commit();
    void cancel();
}
