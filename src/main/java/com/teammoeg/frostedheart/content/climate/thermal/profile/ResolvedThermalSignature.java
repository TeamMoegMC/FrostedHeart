/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile;

/** Whole-block transport property and ordinary/phase material identity. */
public record ResolvedThermalSignature(int ventilation, int materialProfileId,
        int materialStateId, int fullContactFaces, boolean airMatter, int materialTypeId) {
    public ResolvedThermalSignature(int ventilation, int materialProfileId, int materialStateId,
            int fullContactFaces, boolean airMatter) {
        this(ventilation, materialProfileId, materialStateId, fullContactFaces, airMatter, -1);
    }
    public ResolvedThermalSignature(int ventilation, int materialProfileId) {
        this(ventilation, materialProfileId, -1, ventilation == 0 ? 63 : 0,
                ventilation == 100 && materialProfileId == 0);
    }
    public ResolvedThermalSignature {
        if (ventilation < 0 || ventilation > 100 || materialProfileId < 0)
            throw new IllegalArgumentException("invalid block thermal signature");
    }
}
