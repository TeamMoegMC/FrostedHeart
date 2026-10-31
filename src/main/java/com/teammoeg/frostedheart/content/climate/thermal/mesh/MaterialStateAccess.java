/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/**
 * Material values at a worker boundary. Reads use caller-owned scratch; explicit
 * controls run after numerical work and before preparing the next layout.
 */
public interface MaterialStateAccess {
    void readMaterial(int slot, MaterialSample target);

    /** Changes heat at a control boundary, never inside a speculative numerical step. */
    void applyMaterialControl(int slot, double enthalpyJ, byte branch);
}
