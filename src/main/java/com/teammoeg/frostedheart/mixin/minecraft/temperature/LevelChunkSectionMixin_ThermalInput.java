/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalSectionAttachment;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionMixin_ThermalInput
        implements MinecraftThermalSectionAttachment {
    @Unique
    private volatile MinecraftPageManager.SectionOwner frostedheart$thermalInputOwner;

    @Override
    public MinecraftPageManager.SectionOwner frostedheart$getThermalInputOwner() {
        return frostedheart$thermalInputOwner;
    }

    @Override
    public void frostedheart$setThermalInputOwner(
            MinecraftPageManager.SectionOwner owner
    ) {
        frostedheart$thermalInputOwner = owner;
    }

    @Inject(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN")
    )
    private void frostedheart$recordThermalMutation(
            int x, int y, int z, BlockState newState, boolean useLocks,
            CallbackInfoReturnable<BlockState> callback) {
        MinecraftThermalInput.onSectionSetBlockState(
                (LevelChunkSection) (Object) this,
                x, y, z, callback.getReturnValue(), newState);
    }
}
