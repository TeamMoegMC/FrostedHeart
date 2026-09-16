/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/** Caller-owned, reusable material reading. This is not the body's authoritative state. */
public final class MaterialSample {
    public enum Source {
        UNAVAILABLE,
        LIVE,
        STORED
    }

    private boolean stored;
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
        return law == null ? Source.UNAVAILABLE : stored ? Source.STORED : Source.LIVE;
    }

    public double temperatureC() {
        return law == null ? Double.NaN : law.temperatureC(enthalpyJ, branch);
    }

    public void set(double energyJ, MaterialThermalLaw law, byte branch, long sampleTick) {
        stored = false;
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
        stored = true;
    }

    public void clear() {
        law = null;
        enthalpyJ = Double.NaN;
        branch = 0;
        sampleTick = -1;
        requestSequence = 0;
    }
}
