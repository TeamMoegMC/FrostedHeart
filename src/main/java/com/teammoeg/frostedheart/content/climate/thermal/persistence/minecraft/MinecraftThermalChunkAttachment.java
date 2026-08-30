/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

/** Nullable dormant thermal state mixed into full server chunks. */
public interface MinecraftThermalChunkAttachment {
    DormantChunkThermalState frostedheart$getDormantThermalState();

    void frostedheart$setDormantThermalState(DormantChunkThermalState state);
}
