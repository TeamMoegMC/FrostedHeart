/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin_DormantThermal
        implements MinecraftThermalChunkAttachment {
    @Unique
    private DormantChunkThermalState frostedheart$dormantThermalState;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void frostedheart$startLitSource(BlockPos pos, BlockState state, boolean moving,
            CallbackInfoReturnable<BlockState> callback) {
        BlockState previous = callback.getReturnValue();
        if (previous != null && CampfireBlock.isLitCampfire(state)
                && !CampfireBlock.isLitCampfire(previous)
                && ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level) {
            MinecraftThermalInput.onCampfireIgnited(level, pos);
        }
    }

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
