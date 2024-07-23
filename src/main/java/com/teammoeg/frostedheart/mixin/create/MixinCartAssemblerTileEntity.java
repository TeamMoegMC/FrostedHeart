/*
 * Copyright (c) 2021-2024 TeamMoeg
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.components.structureMovement.mounted.CartAssemblerTileEntity;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(CartAssemblerTileEntity.class)
public abstract class MixinCartAssemblerTileEntity extends SmartTileEntity {
    public MixinCartAssemblerTileEntity(BlockEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Shadow(remap = false)
    protected abstract void disassemble(Level world, BlockPos pos, AbstractMinecart cart);

    @Shadow(remap = false)
    public abstract boolean isMinecartUpdateValid();

    @Shadow(remap = false)
    public abstract void resetTicksSinceMinecartUpdate();

    /**
     * @author khjxiaogu
     * @reason config disable cart assembly
     */
    @Inject(at = @At("HEAD"), method = "tryAssemble", remap = false, cancellable = true)
    public void tryAssemble(AbstractMinecart cart, CallbackInfo cbi) {
        if (cart == null)
            return;
        if (!cart.level.isClientSide) {


            if (!isMinecartUpdateValid())
                return;
            resetTicksSinceMinecartUpdate();
            disassemble(level, worldPosition, cart);
            cbi.cancel();

        }

    }
}
