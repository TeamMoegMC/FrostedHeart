/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;

/** Lazy exchange with a constant natural-temperature bath; no world access or ticking. */
public final class DormantThermalCooling {
    private DormantThermalCooling() {}

    public static double rate(double halfLifeSeconds) {
        return 0.6931471805599453 / halfLifeSeconds;
    }

    public static double factor(long savedTick, long tick, double rate) {
        return Math.exp(-rate * (Math.max(0L, tick - savedTick) / 20.0));
    }

    public static double fraction(long savedTick, long tick, double rate) {
        return -Math.expm1(-rate * (Math.max(0L, tick - savedTick) / 20.0));
    }

    public static double temperature(double initialC, double naturalC, double factor) {
        return naturalC + (initialC - naturalC) * factor;
    }

    /** Advances only this BlockState's law. A completed transition waits for world application. */
    public static void project(MaterialSample sample, long tick, double naturalC, double rate) {
        project(sample, tick, naturalC, rate, fraction(sample.sampleTick(), tick, rate));
    }

    /** Batch readers can reuse the sensible fraction for records with the same timestamp. */
    public static void project(
            MaterialSample sample,
            long tick,
            double naturalC,
            double rate,
            double sensibleFraction) {
        double seconds = Math.max(0L, tick - sample.sampleTick()) / 20.0;
        MaterialThermalLaw law = sample.law();
        double energy = sample.enthalpyJ();
        byte branch = sample.branch();
        if (law.heating() == null && law.cooling() == null) {
            energy +=
                    law.capacityJPerK()
                            * (naturalC - law.temperatureC(energy, branch))
                            * sensibleFraction;
            sample.setStored(energy, law, MaterialThermalLaw.SENSIBLE, tick);
            return;
        }
        double conductance = rate * law.capacityJPerK();
        // Reverse plateau, sensible interval, then forward plateau: bounded independently of age.
        for (int segment = 0; segment < 4 && seconds > 0; segment++) {
            double delta = naturalC - law.temperatureC(energy, branch);
            if (delta == 0) break;
            double direction = Math.signum(delta);
            branch = law.selectBranch(energy, branch, direction);
            double limit = law.energyLimitJ(energy, branch, direction);
            if (limit == 0) break;
            delta = naturalC - law.temperatureC(energy, branch);
            if (delta == 0 || Math.signum(delta) != direction) break;
            double slope = law.slopeKPerJ(energy, branch);
            double requested =
                    slope == 0
                            ? Math.abs(delta) * conductance * seconds
                            : Math.abs(delta)
                                    / slope
                                    * (segment == 0
                                            ? sensibleFraction
                                            : -Math.expm1(-rate * seconds));
            double transferred = Math.min(requested, limit);
            energy += direction * transferred;
            if (transferred == requested) break;
            double elapsed =
                    slope == 0
                            ? transferred / (conductance * Math.abs(delta))
                            : -Math.log1p(-transferred * slope / Math.abs(delta)) / rate;
            seconds = Math.max(0, seconds - elapsed);
        }
        sample.setStored(energy, law, branch, tick);
    }
}
