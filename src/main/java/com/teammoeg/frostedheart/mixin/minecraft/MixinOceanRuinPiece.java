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

import com.teammoeg.frostedheart.util.StructureUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

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
            BlockState chest = StructureUtils.getChest().getDefaultState();
            if (chest.hasProperty(BlockStateProperties.WATERLOGGED))
                chest = chest.with(BlockStateProperties.WATERLOGGED, Boolean.valueOf(worldIn.getFluidState(pos).isTagged(FluidTags.WATER)));
            worldIn.setBlockState(pos, chest, 2);
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof LockableLootTileEntity) {
                ((LockableLootTileEntity) tileentity).setLootTable(this.isLarge ? LootTables.CHESTS_UNDERWATER_RUIN_BIG : LootTables.CHESTS_UNDERWATER_RUIN_SMALL, rand.nextLong());
            }
        } else if ("drowned".equals(function)) {
            DrownedEntity drownedentity = EntityType.DROWNED.create(worldIn.getWorld());
            drownedentity.enablePersistence();
            drownedentity.moveToBlockPosAndAngles(pos, 0.0F, 0.0F);
            drownedentity.onInitialSpawn(worldIn, worldIn.getDifficultyForLocation(pos), SpawnReason.STRUCTURE, (ILivingEntityData) null, (CompoundNBT) null);
            worldIn.func_242417_l(drownedentity);
            if (pos.getY() > worldIn.getSeaLevel()) {
                worldIn.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
            } else {
                worldIn.setBlockState(pos, Blocks.WATER.getDefaultState(), 2);
            }
        }

    }

}
