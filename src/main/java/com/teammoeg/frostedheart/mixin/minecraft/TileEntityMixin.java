/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.mixin.IOwnerTile;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;


/**
 * Mixin to set owner for TEs, for research systems.
 */
@Mixin(BlockEntity.class)
public class TileEntityMixin implements IOwnerTile {
    UUID id;

    @Inject(at = @At("RETURN"), method = "load")
    public void fh$to$read(CompoundTag nbt, CallbackInfo cbi) {
        if (nbt.contains("fhowner"))
            id = UUID.fromString(nbt.getString("fhowner"));
    }

    @Inject(at = @At("HEAD"), method = "saveAdditional")
    public void fh$to$write(CompoundTag nbt, CallbackInfo cbi) {
        if (id != null)
            nbt.putString("fhowner", id.toString());
    }

    @Override
    public UUID getStoredOwner() {
        return id;
    }

    @Override
    public void setStoredOwner(UUID id) {
        this.id = id;
    }
}
