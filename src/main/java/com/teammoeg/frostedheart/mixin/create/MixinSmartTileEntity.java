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

package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.simibubi.create.foundation.tileEntity.SmartTileEntity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.World;

@Mixin(SmartTileEntity.class)
public abstract class MixinSmartTileEntity extends TileEntity {

    public MixinSmartTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    /**
     * @author khjxiaogu
     * @reason fixed crash in dev environment.
     */
    @Overwrite(remap = false)
    public World getLevel() {
        return level;
    }
}
