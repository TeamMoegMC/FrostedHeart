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
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.util.mixin.StructureUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.DrownedEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.OceanRuinPieces;
import net.minecraft.world.gen.feature.structure.TemplateStructurePiece;
/**
 * Replace chest to stone ones
 * */
@Mixin(OceanRuinPieces.Piece.class)
public abstract class MixinOceanRuinPiece extends TemplateStructurePiece {
    @Shadow
    boolean isLarge;

    public MixinOceanRuinPiece(IStructurePieceType structurePieceTypeIn, CompoundNBT nbt) {
        super(structurePieceTypeIn, nbt);
    }

    public MixinOceanRuinPiece(IStructurePieceType structurePieceTypeIn, int componentTypeIn) {
        super(structurePieceTypeIn, componentTypeIn);
    }

    /**
     * @author khjxiaogu
     * @reason fix chest type to fit our structure system
     */
    @Overwrite
    protected void handleDataMarker(String function, BlockPos pos, IServerWorld worldIn, Random rand, MutableBoundingBox sbb) {
        if ("chest".equals(function)) {
            BlockState chest = StructureUtils.getChest().defaultBlockState();
            if (chest.hasProperty(BlockStateProperties.WATERLOGGED))
                chest = chest.setValue(BlockStateProperties.WATERLOGGED, worldIn.getFluidState(pos).is(FluidTags.WATER));
            worldIn.setBlock(pos, chest, 2);
            TileEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof LockableLootTileEntity) {
                ((LockableLootTileEntity) tileentity).setLootTable(this.isLarge ? LootTables.UNDERWATER_RUIN_BIG : LootTables.UNDERWATER_RUIN_SMALL, rand.nextLong());
            }
        } else if ("drowned".equals(function)) {
            DrownedEntity drownedentity = EntityType.DROWNED.create(worldIn.getLevel());
            drownedentity.setPersistenceRequired();
            drownedentity.moveTo(pos, 0.0F, 0.0F);
            drownedentity.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(pos), SpawnReason.STRUCTURE, null, null);
            worldIn.addFreshEntityWithPassengers(drownedentity);
            if (pos.getY() > worldIn.getSeaLevel()) {
                worldIn.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            } else {
                worldIn.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
            }
        }

    }

}
