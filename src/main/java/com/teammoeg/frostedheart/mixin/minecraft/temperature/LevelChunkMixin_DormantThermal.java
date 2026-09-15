/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin_DormantThermal
        implements MinecraftThermalChunkAttachment {
    @Unique
    private DormantChunkThermalState frostedheart$dormantThermalState;
    @Unique private long frostedheart$materialRevision;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void frostedheart$updateStoredMaterial(
            BlockPos position, BlockState state, boolean moving,
            CallbackInfoReturnable<BlockState> callback) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        BlockState previous = callback.getReturnValue();

        if (previous != null && previous != state && !chunk.getLevel().isClientSide) {
            MinecraftThermalInput.onMaterialBlockChanged(chunk, position, previous, state);
        }
    }

    @Override
    public DormantChunkThermalState frostedheart$getDormantThermalState() {
        return frostedheart$dormantThermalState;
    }

    @Override
    public long frostedheart$getMaterialRevision() { return frostedheart$materialRevision; }

    @Override
    public void frostedheart$setDormantThermalState(
            DormantChunkThermalState state
    ) {
        frostedheart$dormantThermalState = state;
        frostedheart$materialRevision = DormantChunkThermalState.nextMaterialRevision();
    }
}
