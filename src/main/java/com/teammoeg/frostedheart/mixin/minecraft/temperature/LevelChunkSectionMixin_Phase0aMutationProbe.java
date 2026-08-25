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

import java.util.concurrent.atomic.AtomicLong;

@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionMixin_Phase0aMutationProbe
        implements Phase0aSectionAttachment {
    @Unique
    private volatile Phase0aMutationProbe.LoadedSectionOwner frostedheart$phase0aOwner;
    @Unique
    private AtomicLong frostedheart$phase0aUnmappedWrites;

    @Override
    public Phase0aMutationProbe.LoadedSectionOwner frostedheart$getPhase0aOwner() {
        return this.frostedheart$phase0aOwner;
    }

    @Override
    public void frostedheart$setPhase0aOwner(Phase0aMutationProbe.LoadedSectionOwner owner) {
        this.frostedheart$phase0aOwner = owner;
    }

    @Override
    public long frostedheart$incrementPhase0aUnmappedWrites() {
        AtomicLong counter = frostedheart$phase0aUnmappedWrites;
        if (counter == null) {
            synchronized (this) {
                counter = frostedheart$phase0aUnmappedWrites;
                if (counter == null) {
                    counter = new AtomicLong();
                    frostedheart$phase0aUnmappedWrites = counter;
                }
            }
        }
        return counter.incrementAndGet();
    }

    @Override
    public long frostedheart$getPhase0aUnmappedWrites() {
        AtomicLong counter = frostedheart$phase0aUnmappedWrites;
        return counter == null ? 0L : counter.get();
    }

    @Inject(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN")
    )
    private void frostedheart$observePhase0aMutation(
            int x, int y, int z, BlockState newState, boolean useLocks,
            CallbackInfoReturnable<BlockState> callback) {
        Phase0aMutationProbe.onSectionSetBlockState(
                (LevelChunkSection) (Object) this,
                x, y, z, callback.getReturnValue(), newState);
    }
}
