/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationProbe;
import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aSectionAttachment;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionMixin_Phase0aMutationProbe implements Phase0aSectionAttachment {
    @Unique
    private volatile Phase0aMutationProbe.LoadedSectionOwner frostedheart$phase0aOwner;

    @Override
    public Phase0aMutationProbe.LoadedSectionOwner frostedheart$getPhase0aOwner() {
        return this.frostedheart$phase0aOwner;
    }

    @Override
    public void frostedheart$setPhase0aOwner(Phase0aMutationProbe.LoadedSectionOwner owner) {
        this.frostedheart$phase0aOwner = owner;
    }

    @Inject(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN")
    )
    private void frostedheart$observePhase0aMutation(
            int x, int y, int z, BlockState newState, boolean useLocks,
            CallbackInfoReturnable<BlockState> callback) {
        Phase0aMutationProbe.onSectionSetBlockState(
                (LevelChunkSection) (Object) this, x, y, z, callback.getReturnValue(), newState);
    }
}
