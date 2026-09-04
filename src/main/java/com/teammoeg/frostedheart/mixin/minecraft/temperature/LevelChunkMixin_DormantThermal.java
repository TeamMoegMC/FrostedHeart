/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin_DormantThermal
        implements MinecraftThermalChunkAttachment {
    @Unique
    private DormantChunkThermalState frostedheart$dormantThermalState;

    @Override
    public DormantChunkThermalState frostedheart$getDormantThermalState() {
        return frostedheart$dormantThermalState;
    }

    @Override
    public void frostedheart$setDormantThermalState(
            DormantChunkThermalState state
    ) {
        frostedheart$dormantThermalState = state;
    }
}
