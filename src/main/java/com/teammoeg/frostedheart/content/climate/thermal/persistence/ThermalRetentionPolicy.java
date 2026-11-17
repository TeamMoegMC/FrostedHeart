/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;

/** Gameplay-sized residuals; latent energy and unconsumed input have separate owners. */
public final class ThermalRetentionPolicy {
    public static final double ENTER_C = 1.0;
    public static final double EXIT_C = 0.5;

    private ThermalRetentionPolicy() { }

    public static boolean retains(double residualC) {
        return !Double.isFinite(residualC) || Math.abs(residualC) >= EXIT_C;
    }

    public static boolean retains(MaterialThermalLaw law, double energyJ, byte branch, double naturalC) {
        var transition = law.transition(branch);
        return transition != null && transition.progress(energyJ) > 0
                || retains(law.temperatureC(energyJ, branch) - naturalC);
    }
}
