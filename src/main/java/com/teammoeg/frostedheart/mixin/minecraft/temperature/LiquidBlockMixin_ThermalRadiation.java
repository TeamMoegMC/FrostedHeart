/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks only lava's own aggregate Brick after a direct-neighbor shape update. */
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin_ThermalRadiation {
    @Unique private static final byte FROSTEDHEART_UNKNOWN = 0;
    @Unique private static final byte FROSTEDHEART_NOT_LAVA = 1;
    @Unique private static final byte FROSTEDHEART_LAVA = 2;

    @Unique
    private byte frostedheart$thermalRadiationKind;
    @Unique
    private int frostedheart$thermalRadiationEpoch;

    @Inject(
            method = "updateShape",
            at = @At("HEAD")
    )
    private void frostedheart$markLavaRadiationBrick(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos currentPosition,
            BlockPos neighborPosition,
            CallbackInfoReturnable<BlockState> callback
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int epoch = MinecraftThermalProfiles.profileEpoch();
        byte kind = frostedheart$thermalRadiationKind;
        if (kind == FROSTEDHEART_UNKNOWN
                || frostedheart$thermalRadiationEpoch != epoch) {
            kind = state.getFluidState().is(FluidTags.LAVA)
                    ? FROSTEDHEART_LAVA : FROSTEDHEART_NOT_LAVA;
            frostedheart$thermalRadiationKind = kind;
            frostedheart$thermalRadiationEpoch = epoch;
        }
        if (kind == FROSTEDHEART_LAVA
                && MinecraftThermalProfiles.lavaBlockRadiationEnabled()) {
            MinecraftThermalInput.onRadiantLiquidNeighborChanged(
                    server, currentPosition);
        }
    }
}
