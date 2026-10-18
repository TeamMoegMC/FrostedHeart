/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment;
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
        BlockState previous = callback.getReturnValue();
        if (frostedheart$thermalInputOwner != null && previous != newState) {
            MinecraftThermalInput.onSectionSetBlockState(
                    (LevelChunkSection) (Object) this, x, y, z, previous, newState);
        }
    }
}
