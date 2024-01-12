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

import com.simibubi.create.content.contraptions.components.structureMovement.mounted.CartAssemblerTileEntity;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CartAssemblerTileEntity.class)
public abstract class MixinCartAssemblerTileEntity extends SmartTileEntity {
    public MixinCartAssemblerTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    /**
     * @author khjxiaogu
     * @reason config disable cart assembly
     */
    @Inject(at = @At("HEAD"), method = "tryAssemble", remap = false, cancellable = true)
    public void tryAssemble(AbstractMinecartEntity cart, CallbackInfo cbi) {
        if (cart == null)
            return;
        if (!cart.world.isRemote) {


            if (!isMinecartUpdateValid())
                return;
            resetTicksSinceMinecartUpdate();
            disassemble(world, pos, cart);
            cbi.cancel();

        }

    }

    @Shadow(remap = false)
    protected abstract void disassemble(World world, BlockPos pos, AbstractMinecartEntity cart);

    @Shadow(remap = false)
    public abstract void resetTicksSinceMinecartUpdate();

    @Shadow(remap = false)
    public abstract boolean isMinecartUpdateValid();
}
