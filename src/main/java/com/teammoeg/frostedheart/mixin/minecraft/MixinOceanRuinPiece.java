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

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.OceanRuinPieces;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
/**
 * Replace chest to stone ones
 * */
@Mixin(OceanRuinPieces.OceanRuinPiece.class)
public abstract class MixinOceanRuinPiece extends TemplateStructurePiece {
    @Shadow
    boolean isLarge;

    public MixinOceanRuinPiece(StructurePieceType structurePieceTypeIn, CompoundTag nbt) {
        super(structurePieceTypeIn, nbt);
    }

    public MixinOceanRuinPiece(StructurePieceType structurePieceTypeIn, int componentTypeIn) {
        super(structurePieceTypeIn, componentTypeIn);
    }

    /**
     * @author khjxiaogu
     * @reason fix chest type to fit our structure system
     */
    @Overwrite
    protected void handleDataMarker(String function, BlockPos pos, ServerLevelAccessor worldIn, Random rand, BoundingBox sbb) {
        if ("chest".equals(function)) {
            BlockState chest = StructureUtils.getChest().defaultBlockState();
            if (chest.hasProperty(BlockStateProperties.WATERLOGGED))
                chest = chest.setValue(BlockStateProperties.WATERLOGGED, worldIn.getFluidState(pos).is(FluidTags.WATER));
            worldIn.setBlock(pos, chest, 2);
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof RandomizableContainerBlockEntity) {
                ((RandomizableContainerBlockEntity) tileentity).setLootTable(this.isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL, rand.nextLong());
            }
        } else if ("drowned".equals(function)) {
            Drowned drownedentity = EntityType.DROWNED.create(worldIn.getLevel());
            drownedentity.setPersistenceRequired();
            drownedentity.moveTo(pos, 0.0F, 0.0F);
            drownedentity.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
            worldIn.addFreshEntityWithPassengers(drownedentity);
            if (pos.getY() > worldIn.getSeaLevel()) {
                worldIn.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            } else {
                worldIn.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
            }
        }

    }

}
