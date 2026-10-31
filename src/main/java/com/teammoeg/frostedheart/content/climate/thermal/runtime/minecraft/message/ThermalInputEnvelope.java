/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;

/** Main-to-coordinator ownership transfer; world controls are split once before transport. */
public record ThermalInputEnvelope(long dimensionGeneration, long sequence, long targetTick,
        ThermalPageInput[] pageInputs, SpatialSourceTimeline.Window[] sourceWindows,
        double farFieldConductanceScale) {
    public ThermalInputEnvelope(ThermalInputBatch controls, SpatialSourceTimeline.Window[] sources) {
        this(controls.dimensionGeneration(), controls.sequence(), controls.targetTick(),
                ThermalPageInput.split(controls), sources, controls.farFieldConductanceScale());
    }

    public boolean hasFarFieldConductanceScale() { return !Double.isNaN(farFieldConductanceScale); }
}
