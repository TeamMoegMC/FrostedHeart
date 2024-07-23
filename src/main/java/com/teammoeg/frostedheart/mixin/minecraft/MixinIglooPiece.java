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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.IglooPieces;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
/**
 * Replace chest to stone ones
 * */
@Mixin(IglooPieces.IglooPiece.class)
public abstract class MixinIglooPiece extends TemplateStructurePiece {

    public MixinIglooPiece(StructurePieceType structurePieceTypeIn, CompoundTag nbt) {
        super(structurePieceTypeIn, nbt);
    }

    public MixinIglooPiece(StructurePieceType structurePieceTypeIn, int componentTypeIn) {
        super(structurePieceTypeIn, componentTypeIn);
    }

    /**
     * @author khjxiaogu
     * @reason fix chest type to fit our structure system
     */
    @Overwrite
    protected void handleDataMarker(String function, BlockPos pos, ServerLevelAccessor worldIn, Random rand, BoundingBox sbb) {
        if ("chest".equals(function)) {
            worldIn.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            BlockEntity tileentity = worldIn.getBlockEntity(pos.below());
            if (tileentity instanceof RandomizableContainerBlockEntity) {
                ((RandomizableContainerBlockEntity) tileentity).setLootTable(BuiltInLootTables.IGLOO_CHEST, rand.nextLong());
            }

        }
    }

}
