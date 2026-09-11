/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile;

/** Whole-block transport property and ordinary/phase material identity. */
public record ResolvedThermalSignature(int ventilation, int materialProfileId) {
    public ResolvedThermalSignature {
        if (ventilation < 0 || ventilation > 100 || materialProfileId < 0)
            throw new IllegalArgumentException("invalid block thermal signature");
    }
}
