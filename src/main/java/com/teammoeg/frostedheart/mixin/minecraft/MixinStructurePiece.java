/*
 * Copyright (c) 2022 TeamMoeg
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

import java.util.Random;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.util.StructureUtils;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.feature.structure.StructurePiece;

@Mixin(StructurePiece.class)
public class MixinStructurePiece {
    /**
     * @author khjxiaogu
     * @reason auto remake chests
     */
    @Overwrite
    protected boolean generateChest(IServerWorld worldIn, MutableBoundingBox boundsIn, Random rand, BlockPos posIn,
                                    ResourceLocation resourceLocationIn, @Nullable BlockState p_191080_6_) {
        if (boundsIn.isVecInside(posIn) && !worldIn.getBlockState(posIn).matchesBlock(StructureUtils.getChest())) {
            if (p_191080_6_ == null) {
                p_191080_6_ = StructurePiece.correctFacing(worldIn, posIn, StructureUtils.getChest().getDefaultState());
            }

            worldIn.setBlockState(posIn, p_191080_6_, 2);
            TileEntity tileentity = worldIn.getTileEntity(posIn);
            if (tileentity instanceof LockableLootTileEntity) {
                ((LockableLootTileEntity) tileentity).setLootTable(resourceLocationIn, rand.nextLong());
            }

            return true;
        }
        return false;
    }

    @Inject(at = @At("HEAD"), method = "setBlockState", cancellable = true)
    protected void setBlockState(ISeedReader worldIn, BlockState blockstateIn, int x, int y, int z, MutableBoundingBox boundingboxIn, CallbackInfo cbi) {
        if (StructureUtils.isBanned(blockstateIn.getBlock())) {
            cbi.cancel();
        }
    }
}
