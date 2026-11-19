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
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/** Caller-owned, reusable material reading. This is not the body's authoritative state. */
public final class MaterialSample {
    public enum Source {
        UNAVAILABLE,
        ESTIMATED,
        LIVE,
        STORED
    }

    private Source source = Source.UNAVAILABLE;
    private double estimateC;
    private double enthalpyJ;
    private MaterialThermalLaw law;
    private byte branch;
    private long sampleTick;
    private long requestSequence;

    public double enthalpyJ() {
        return enthalpyJ;
    }

    public MaterialThermalLaw law() {
        return law;
    }

    public byte branch() {
        return branch;
    }

    public long sampleTick() {
        return sampleTick;
    }

    public long requestSequence() {
        return requestSequence;
    }

    public Source source() {
        return source;
    }

    public double temperatureC() {
        return source == Source.ESTIMATED ? estimateC : law == null ? Double.NaN : law.temperatureC(enthalpyJ, branch);
    }

    public void set(double energyJ, MaterialThermalLaw law, byte branch, long sampleTick) {
        source = law == null ? Source.UNAVAILABLE : Source.LIVE;
        this.enthalpyJ = energyJ;
        this.law = law;
        this.branch = branch;
        this.sampleTick = sampleTick;
        requestSequence = 0;
    }

    /** Install all fields only after the publication reader has validated its version. */
    public void setLive(
            double energyJ,
            MaterialThermalLaw law,
            byte branch,
            long sampleTick,
            long requestSequence) {
        set(energyJ, law, branch, sampleTick);
        this.requestSequence = requestSequence;
    }

    public void setStored(double energyJ, MaterialThermalLaw law, byte branch, long sampleTick) {
        set(energyJ, law, branch, sampleTick);
        source = Source.STORED;
    }

    /** Report an unrecorded body at environmental equilibrium without creating stored energy. */
    public void estimate(double temperatureC) {
        clear();
        estimateC = temperatureC;
        source = Source.ESTIMATED;
    }

    public void clear() {
        source = Source.UNAVAILABLE;
        law = null;
        enthalpyJ = Double.NaN;
        branch = 0;
        sampleTick = -1;
        requestSequence = 0;
    }
}
