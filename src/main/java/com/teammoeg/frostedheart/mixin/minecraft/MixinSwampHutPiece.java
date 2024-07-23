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

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SwamplandHutPiece;
/**
 * Remove crafting table in swamp hut
 * */
@Mixin(SwamplandHutPiece.class)
public abstract class MixinSwampHutPiece extends ScatteredFeaturePiece {

    protected MixinSwampHutPiece(StructurePieceType structurePieceTypeIn, CompoundTag nbt) {
        super(structurePieceTypeIn, nbt);
    }

    public MixinSwampHutPiece(StructurePieceType structurePieceTypeIn, Random rand, int xIn, int yIn, int zIn,
                              int widthIn, int heightIn, int depthIn) {
        super(structurePieceTypeIn, rand, xIn, yIn, zIn, widthIn, heightIn, depthIn);
    }

    @Override
    protected void placeBlock(WorldGenLevel worldIn, BlockState blockstateIn, int x, int y, int z,
                                 BoundingBox boundingboxIn) {
        if (blockstateIn != null && blockstateIn.getBlock() instanceof CraftingTableBlock) return;

        super.placeBlock(worldIn, blockstateIn, x, y, z, boundingboxIn);
    }

}
