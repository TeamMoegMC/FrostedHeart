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

import java.util.Random;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.util.mixin.StructureUtils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
/**
 * Replace chest to stone ones
 * */
@Mixin(StructurePiece.class)
public class MixinStructurePiece {
    /**
     * @author khjxiaogu
     * @reason auto remake chests
     */
    @Overwrite
    protected boolean generateChest(ServerLevelAccessor worldIn, BoundingBox boundsIn, Random rand, BlockPos posIn,
                                    ResourceLocation resourceLocationIn, @Nullable BlockState p_191080_6_) {
        if (boundsIn.isInside(posIn) && !worldIn.getBlockState(posIn).is(StructureUtils.getChest())) {
            if (p_191080_6_ == null) {
                p_191080_6_ = StructurePiece.reorient(worldIn, posIn, StructureUtils.getChest().defaultBlockState());
            }

            worldIn.setBlock(posIn, p_191080_6_, 2);
            BlockEntity tileentity = worldIn.getBlockEntity(posIn);
            if (tileentity instanceof RandomizableContainerBlockEntity) {
                ((RandomizableContainerBlockEntity) tileentity).setLootTable(resourceLocationIn, rand.nextLong());
            }

            return true;
        }
        return false;
    }

    @Inject(at = @At("HEAD"), method = "setBlockState", cancellable = true)
    protected void setBlockState(WorldGenLevel worldIn, BlockState blockstateIn, int x, int y, int z, BoundingBox boundingboxIn, CallbackInfo cbi) {
        if (StructureUtils.isBanned(blockstateIn.getBlock())) {
            cbi.cancel();
        }
    }
}
